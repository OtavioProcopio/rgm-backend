package com.rgm.api.adapter.out.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rgm.api.adapter.out.persistence.SolicitacaoRepositoryAdapter;
import com.rgm.api.core.application.usecases.solicitacao.ObterPainelPorPeriodoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.ObterPainelPorPeriodoUseCase.Output;
import java.time.Clock;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class PainelPostgresTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("rgm_test");

  private static final int SOLICITACOES = 700;
  private static final int MODELOS = 50;
  private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

  @Autowired private SolicitacaoJpaRepository repository;
  @Autowired private JdbcTemplate jdbc;

  private ObterPainelPorPeriodoUseCase painel;

  @DynamicPropertySource
  static void configurar(final DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    registry.add("spring.flyway.enabled", () -> "true");
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
  }

  @BeforeEach
  void setUp() {
    final SolicitacaoRepositoryAdapter adapter = new SolicitacaoRepositoryAdapter(repository);
    painel = new ObterPainelPorPeriodoUseCase(adapter, FUSO, Clock.systemUTC());
    semearSolicitacoesNosUltimos90Dias();
  }

  private void semearSolicitacoesNosUltimos90Dias() {
    final UUID usuarioId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO usuarios (id, nome, perfil) VALUES (?, 'Usuario painel', 'OPERADOR')",
        usuarioId);
    jdbc.update(
        "INSERT INTO modelos (id, codigo, descricao, maquina) "
            + "SELECT gen_random_uuid(), 'PAINEL-' || g, 'Modelo painel', 'FBOX' "
            + "FROM generate_series(1, ?) g",
        MODELOS);
    jdbc.update(
        "INSERT INTO solicitacoes (id, titulo, tipo, status, prioridade, modelo_id, "
            + "aberta_por_usuario_id, criada_em, atualizada_em, concluida_em, cancelada_em) "
            + "SELECT gen_random_uuid(), 'Sintetica ' || g, "
            + "(ARRAY['REPARO','INSPECAO','REENGENHARIA'])[1 + (g / 3) % 3], "
            + "(ARRAY['A_FAZER','EM_ANDAMENTO','EM_VALIDACAO','CONCLUIDA','CANCELADA'])[1 + g % 5], "
            + "(ARRAY['BAIXA','MEDIA','ALTA','URGENTE',NULL,NULL])[1 + g % 6], "
            + "m.id, ?, c.criada_em, c.criada_em, "
            + "CASE WHEN g % 5 = 3 THEN LEAST(c.criada_em + ((g % 600) + 1) * INTERVAL '1 minute', now()) END, "
            + "CASE WHEN g % 5 = 4 THEN LEAST(c.criada_em + ((g % 600) + 1) * INTERVAL '1 minute', now()) END "
            + "FROM generate_series(1, ?) g "
            + "CROSS JOIN LATERAL (SELECT now() - g * INTERVAL '3 hours' AS criada_em) c "
            + "JOIN (SELECT id, row_number() OVER (ORDER BY codigo) - 1 AS rn FROM modelos "
            + "WHERE codigo LIKE 'PAINEL-%') m ON m.rn = g % ?",
        usuarioId, SOLICITACOES, MODELOS);
    jdbc.execute("ANALYZE solicitacoes");
  }

  private long totalDeCriadasNaJanela(final Output saida) {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM solicitacoes WHERE criada_em >= ? AND criada_em < ?",
        Long.class,
        java.sql.Timestamp.from(saida.periodo().inicio()),
        java.sql.Timestamp.from(saida.periodo().fim()));
  }

  @Test
  void shouldSomarTodasAsCriadasNaSerieWhenPainelDe90Dias() {
    // Arrange
    final var input = new ObterPainelPorPeriodoUseCase.Input(90, null);

    // Act
    final Output saida = painel.execute(input);

    // Assert
    final long somaDaSerie =
        saida.periodo().serie().stream()
            .mapToLong(ObterPainelPorPeriodoUseCase.Ponto::criadas)
            .sum();
    assertEquals(SOLICITACOES, totalDeCriadasNaJanela(saida));
    assertEquals(SOLICITACOES, saida.periodo().criadas());
    assertEquals(SOLICITACOES, somaDaSerie);
  }

  @Test
  void shouldSomarCriadasDaSerieIgualAoTotalDaJanelaWhenPainelDe7Dias() {
    // Arrange
    final var input = new ObterPainelPorPeriodoUseCase.Input(7, null);

    // Act
    final Output saida = painel.execute(input);

    // Assert
    final long somaDaSerie =
        saida.periodo().serie().stream()
            .mapToLong(ObterPainelPorPeriodoUseCase.Ponto::criadas)
            .sum();
    assertTrue(saida.periodo().criadas() > 0, "janela de 7 dias nao pode estar vazia");
    assertEquals(totalDeCriadasNaJanela(saida), saida.periodo().criadas());
    assertEquals(saida.periodo().criadas(), somaDaSerie);
  }

  @Test
  void shouldTerUmPontoPorDiaWhenPainelDe30Dias() {
    // Arrange
    final var input = new ObterPainelPorPeriodoUseCase.Input(30, null);

    // Act
    final Output saida = painel.execute(input);

    // Assert
    assertEquals(30, saida.periodo().serie().size());
  }
}
