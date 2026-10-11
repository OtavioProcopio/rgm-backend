package com.rgm.api.adapter.out.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rgm.api.adapter.out.persistence.SolicitacaoRepositoryAdapter;
import com.rgm.api.core.application.usecases.solicitacao.ObterHistoricoMetricasUseCase;
import com.rgm.api.core.application.usecases.solicitacao.ObterPainelPorPeriodoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.ObterPainelPorPeriodoUseCase.Output;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Supplier;
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
class PainelDesempenhoPostgresTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("rgm_test");

  private static final int SOLICITACOES = 10_000;
  private static final int MODELOS = 50;
  private static final int AQUECIMENTOS = 5;
  private static final int MEDIDAS = 30;
  private static final double LIMITE_P95_MS = 500.0;
  private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

  @Autowired private SolicitacaoJpaRepository repository;
  @Autowired private JdbcTemplate jdbc;

  private ObterPainelPorPeriodoUseCase painel;
  private ObterHistoricoMetricasUseCase historico;

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
    historico = new ObterHistoricoMetricasUseCase(adapter, FUSO, Clock.systemUTC());
    semearDezMilSolicitacoesEmLoteNosUltimos90Dias();
  }

  private void semearDezMilSolicitacoesEmLoteNosUltimos90Dias() {
    final UUID usuarioId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO usuarios (id, nome, perfil) VALUES (?, 'Usuario perf', 'OPERADOR')",
        usuarioId);
    jdbc.update(
        "INSERT INTO modelos (id, codigo, descricao, maquina) "
            + "SELECT gen_random_uuid(), 'PERF-' || g, 'Modelo perf', 'FBOX' "
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
            + "CROSS JOIN LATERAL (SELECT now() - g * INTERVAL '760 seconds' AS criada_em) c "
            + "JOIN (SELECT id, row_number() OVER (ORDER BY codigo) - 1 AS rn FROM modelos "
            + "WHERE codigo LIKE 'PERF-%') m ON m.rn = g % ?",
        usuarioId, SOLICITACOES, MODELOS);
    jdbc.execute("ANALYZE solicitacoes");
  }

  private double[] medir(final Supplier<Object> consulta) {
    for (int i = 0; i < AQUECIMENTOS; i++) {
      consulta.get();
    }
    final double[] tempos = new double[MEDIDAS];
    for (int i = 0; i < MEDIDAS; i++) {
      final long inicio = System.nanoTime();
      consulta.get();
      tempos[i] = (System.nanoTime() - inicio) / 1_000_000.0;
    }
    Arrays.sort(tempos);
    return tempos;
  }

  private static double percentilPorPostoMaisProximo(final double[] ordenado, final double p) {
    final int posto = (int) Math.ceil(p / 100.0 * ordenado.length);
    return ordenado[Math.max(0, posto - 1)];
  }

  private void assertP95DentroDoLimite(final String caso, final double[] tempos) {
    final double p95 = percentilPorPostoMaisProximo(tempos, 95);
    assertTrue(p95 <= LIMITE_P95_MS, caso + ": p95=" + p95 + " ms excede " + LIMITE_P95_MS + " ms");
  }

  private long totalDeCriadasNaJanela(final Output saida) {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM solicitacoes WHERE criada_em >= ? AND criada_em < ?",
        Long.class,
        java.sql.Timestamp.from(saida.periodo().inicio()),
        java.sql.Timestamp.from(saida.periodo().fim()));
  }

  @Test
  void shouldFicarAbaixoDe500msNoP95WhenPainelDe7DiasCom10milSolicitacoes() {
    // Arrange
    final var input = new ObterPainelPorPeriodoUseCase.Input(7, null);

    // Act
    final double[] tempos = medir(() -> painel.execute(input));

    // Assert
    assertP95DentroDoLimite("painel dias=7 (diario)", tempos);
  }

  @Test
  void shouldFicarAbaixoDe500msNoP95WhenPainelDe30DiasCom10milSolicitacoes() {
    // Arrange
    final var input = new ObterPainelPorPeriodoUseCase.Input(30, null);

    // Act
    final double[] tempos = medir(() -> painel.execute(input));

    // Assert
    assertP95DentroDoLimite("painel dias=30 (diario)", tempos);
  }

  @Test
  void shouldFicarAbaixoDe500msNoP95WhenPainelDe90DiasCom10milSolicitacoes() {
    // Arrange
    final var input = new ObterPainelPorPeriodoUseCase.Input(90, null);

    // Act
    final double[] tempos = medir(() -> painel.execute(input));

    // Assert
    assertP95DentroDoLimite("painel dias=90 (semanal)", tempos);
  }

  @Test
  void shouldFicarAbaixoDe500msNoP95WhenHistoricoDe365DiasCom10milSolicitacoes() {
    // Arrange
    final var input = new ObterHistoricoMetricasUseCase.Input(365, null);

    // Act
    final double[] tempos = medir(() -> historico.execute(input));

    // Assert
    assertP95DentroDoLimite("historico dias=365 (semanal)", tempos);
  }

  @Test
  void shouldSomarTodasAsCriadasNaSerieWhenPainelDe90DiasCom10milSolicitacoes() {
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
