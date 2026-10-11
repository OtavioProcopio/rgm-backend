package com.rgm.api.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Valida as migrations do Flyway em PostgreSQL real. */
@Testcontainers
class FlywayMigrationTest {

  @Container
  static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("rgm_test");

  @Test
  void migrationV1DeveExecutarComSucesso() {
    final DataSource ds =
        DataSourceBuilder.create()
            .url(postgres.getJdbcUrl())
            .username(postgres.getUsername())
            .password(postgres.getPassword())
            .driverClassName("org.postgresql.Driver")
            .build();

    final Flyway flyway =
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load();

    final var result = flyway.migrate();

    assertTrue(result.success, "Flyway migration deveria ter sucesso");
    assertTrue(result.migrationsExecuted > 0, "Ao menos 1 migration deveria ter sido executada");
  }

  @Test
  void migrationV11DeveSanearDuplicatasECriarIndiceUnicoEFuncaoSemAcento() throws SQLException {
    // Arrange
    final DataSource ds = dataSource("rgm_v11");
    Flyway.configure()
        .dataSource(ds)
        .locations("classpath:db/migration")
        .target("10")
        .load()
        .migrate();
    try (Connection c = ds.getConnection();
        Statement st = c.createStatement()) {
      st.execute(
          "INSERT INTO modelos (id, codigo, versao, descricao, maquina, criado_em) VALUES "
              + "('00000000-0000-0000-0000-000000000001', 'X', 1, 'a', 'M1', now() - interval '3 day'),"
              + "('00000000-0000-0000-0000-000000000002', 'X', 1, 'b', 'M1', now() - interval '2 day'),"
              + "('00000000-0000-0000-0000-000000000003', 'X', 1, 'c', 'M1', now() - interval '1 day'),"
              + "('00000000-0000-0000-0000-000000000004', 'Y', 1, 'd', 'M1', now()),"
              + "('00000000-0000-0000-0000-000000000005', 'Y', 3, 'e', 'M1', now()),"
              + "('00000000-0000-0000-0000-000000000006', 'X', 1, 'f', 'M2', now())");
    }

    // Act
    Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();

    // Assert
    try (Connection c = ds.getConnection();
        Statement st = c.createStatement()) {
      assertEquals(
          List.of("X/1", "X/2", "X/3", "Y/1", "Y/3"), versoes(st, "M1"), "so duplicatas mudam");
      assertEquals(List.of("X/1"), versoes(st, "M2"));
      assertEquals(
          "00000000-0000-0000-0000-000000000001",
          unico(st, "SELECT id FROM modelos WHERE maquina = 'M1' AND codigo = 'X' AND versao = 1"));
      assertEquals(6, Integer.parseInt(unico(st, "SELECT count(*) FROM modelos")));
      assertThrows(
          SQLException.class,
          () ->
              st.execute(
                  "INSERT INTO modelos (codigo, versao, descricao, maquina) "
                      + "VALUES ('Y', 3, 'z', 'M1')"));
      assertEquals("zelia cao", unico(st, "SELECT rgm_sem_acento('Zélia ÇÃO')"));
    }
  }

  @Test
  void migrationV12DeveCriarOCatalogoDeTiposComOsCincoTiposEPreservarOsValoresEmUso()
      throws SQLException {
    // Arrange
    final DataSource ds = dataSource("rgm_v12");
    Flyway.configure()
        .dataSource(ds)
        .locations("classpath:db/migration")
        .target("11")
        .load()
        .migrate();
    try (Connection c = ds.getConnection();
        Statement st = c.createStatement()) {
      st.execute(
          "INSERT INTO modelos (id, codigo, versao, descricao, maquina, tipo) VALUES "
              + "('00000000-0000-0000-0000-0000000000a1', 'A', 1, 'a', 'M1', 'RESINA'),"
              + "('00000000-0000-0000-0000-0000000000a2', 'B', 1, 'b', 'M1', 'TIPO_ANTIGO')");
    }

    // Act
    Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();

    // Assert
    try (Connection c = ds.getConnection();
        Statement st = c.createStatement()) {
      assertEquals("6", unico(st, "SELECT count(*) FROM tipos_modelo"));
      assertEquals(
          "TIPO_ANTIGO", unico(st, "SELECT nome FROM tipos_modelo WHERE nome = 'TIPO_ANTIGO'"));
      assertEquals("RESINA", unico(st, "SELECT tipo FROM modelos WHERE codigo = 'A'"));
      assertThrows(
          SQLException.class,
          () ->
              st.execute(
                  "INSERT INTO tipos_modelo (id, nome) VALUES (gen_random_uuid(), 'RESINA')"));
    }
  }

  private static List<String> versoes(final Statement st, final String maquina)
      throws SQLException {
    final List<String> r = new ArrayList<>();
    try (ResultSet rs =
        st.executeQuery(
            "SELECT codigo || '/' || versao FROM modelos WHERE maquina = '"
                + maquina
                + "' ORDER BY codigo, versao")) {
      while (rs.next()) {
        r.add(rs.getString(1));
      }
    }
    return r;
  }

  private static String unico(final Statement st, final String sql) throws SQLException {
    try (ResultSet rs = st.executeQuery(sql)) {
      rs.next();
      return rs.getString(1);
    }
  }

  private static DataSource dataSource(final String banco) {
    try (Connection c =
            DataSourceBuilder.create()
                .url(postgres.getJdbcUrl())
                .username(postgres.getUsername())
                .password(postgres.getPassword())
                .driverClassName("org.postgresql.Driver")
                .build()
                .getConnection();
        Statement st = c.createStatement()) {
      st.execute("CREATE DATABASE " + banco);
    } catch (final SQLException e) {
      throw new IllegalStateException(e);
    }
    return DataSourceBuilder.create()
        .url(postgres.getJdbcUrl().replace("/rgm_test", "/" + banco))
        .username(postgres.getUsername())
        .password(postgres.getPassword())
        .driverClassName("org.postgresql.Driver")
        .build();
  }
}
