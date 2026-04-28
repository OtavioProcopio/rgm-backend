package com.rgm.api.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.*;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Valida que a migration V1 do Flyway executa sem erros em PostgreSQL real. */
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
}
