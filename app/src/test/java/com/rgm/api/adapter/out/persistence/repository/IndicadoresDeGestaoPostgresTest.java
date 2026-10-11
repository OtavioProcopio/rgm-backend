package com.rgm.api.adapter.out.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rgm.api.adapter.out.persistence.IndicadoresDeGestaoRepositoryAdapter;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.ports.repositories.IndicadoresDeGestaoRepository;
import com.rgm.api.core.domain.ports.repositories.IndicadoresDeGestaoRepository.TempoDeEtapa;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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
class IndicadoresDeGestaoPostgresTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("rgm_test");

  @Autowired private IndicadoresDeGestaoJpaRepository jpa;
  @Autowired private JdbcTemplate jdbc;

  private IndicadoresDeGestaoRepository repositorio;
  private Instant agora;
  private Instant inicio;
  private Instant fim;
  private UUID autor;
  private UUID modelo;

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
    repositorio = new IndicadoresDeGestaoRepositoryAdapter(jpa);
    agora = Instant.now();
    inicio = agora.minus(Duration.ofDays(30));
    fim = agora.plus(Duration.ofHours(1));
    autor = usuario("Autor");
    modelo = modelo("FBOX", "MOD-A");
  }

  // ── dados ────────────────────────────────────────────────────────────────

  private UUID usuario(final String nome) {
    final UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO usuarios (id, nome, perfil) VALUES (?, ?, 'OPERADOR')", id, nome + " " + id);
    return id;
  }

  private UUID modelo(final String maquina, final String codigo) {
    final UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO modelos (id, codigo, descricao, maquina) VALUES (?, ?, 'Modelo', ?)",
        id,
        codigo,
        maquina);
    return id;
  }

  private UUID solicitacao(
      final String tipo,
      final String status,
      final String prioridade,
      final UUID modeloId,
      final Instant criadaEm,
      final Instant concluidaEm) {
    final UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO solicitacoes (id, titulo, tipo, status, prioridade, modelo_id, "
            + "aberta_por_usuario_id, criada_em, atualizada_em, concluida_em) "
            + "VALUES (?, 'T', ?, ?, ?, ?, ?, ?, ?, ?)",
        id,
        tipo,
        status,
        prioridade,
        modeloId,
        autor,
        Timestamp.from(criadaEm),
        Timestamp.from(criadaEm),
        concluidaEm == null ? null : Timestamp.from(concluidaEm));
    return id;
  }

  private UUID concluida(final String prioridade, final Duration tempo) {
    final Instant criada = agora.minus(Duration.ofDays(5));
    return solicitacao("REPARO", "CONCLUIDA", prioridade, modelo, criada, criada.plus(tempo));
  }

  private UUID aberta(final String status, final String prioridade, final Duration idade) {
    return solicitacao("REPARO", status, prioridade, modelo, agora.minus(idade), null);
  }

  private void mudanca(
      final UUID solicitacao, final String de, final String para, final Instant quando) {
    jdbc.update(
        "INSERT INTO atividades_solicitacao (id, solicitacao_id, tipo, de_status, para_status, "
            + "autor_usuario_id, criada_em) VALUES (?, ?, 'MUDANCA_STATUS', ?, ?, ?, ?)",
        UUID.randomUUID(),
        solicitacao,
        de,
        para,
        autor,
        Timestamp.from(quando));
  }

  private void atribuir(final UUID solicitacao, final UUID responsavel) {
    jdbc.update(
        "INSERT INTO solicitacao_atribuicoes (id, solicitacao_id, usuario_id, "
            + "atribuido_por_usuario_id) VALUES (?, ?, ?, ?)",
        UUID.randomUUID(),
        solicitacao,
        responsavel,
        autor);
  }

  // ── prazo e resolução ────────────────────────────────────────────────────

  @Test
  void shouldCountOneMinuteBeforeDeadlineAsOnTimeAndOneMinuteAfterAsLate() {
    // Arrange (ALTA = 24 h)
    concluida("ALTA", Duration.ofHours(24).minusMinutes(1));
    concluida("ALTA", Duration.ofHours(24).plusMinutes(1));

    // Act
    final var linhas = repositorio.prazoPorPrioridade(inicio, fim);

    // Assert
    assertEquals(1, linhas.size());
    assertEquals(PrioridadeSolicitacao.ALTA, linhas.get(0).prioridade());
    assertEquals(2, linhas.get(0).concluidas());
    assertEquals(1, linhas.get(0).noPrazo());
  }

  @Test
  void shouldKeepCompletedWithoutPriorityOutOfTheDeadlineRate() {
    // Arrange
    concluida("BAIXA", Duration.ofHours(1));
    concluida(null, Duration.ofHours(1));

    // Act
    final var linhas = repositorio.prazoPorPrioridade(inicio, fim);
    final long semPrioridade = repositorio.concluidasSemPrioridade(inicio, fim);

    // Assert
    assertEquals(1, linhas.size());
    assertEquals(1, semPrioridade);
  }

  @Test
  void shouldIgnoreCompletedOutsideTheWindow() {
    // Arrange
    concluida("ALTA", Duration.ofHours(1));
    final Instant velha = agora.minus(Duration.ofDays(60));
    solicitacao("REPARO", "CONCLUIDA", "ALTA", modelo, velha, velha.plusSeconds(60));

    // Act
    final var resolucao = repositorio.resolucao(inicio, fim);

    // Assert
    assertEquals(1, resolucao.concluidas());
  }

  @Test
  void shouldComputeMedianAndNinetiethPercentileOfResolutionTime() {
    // Arrange: 1 h a 10 h
    for (int h = 1; h <= 10; h++) {
      concluida("MEDIA", Duration.ofHours(h));
    }

    // Act
    final var resolucao = repositorio.resolucao(inicio, fim);

    // Assert
    assertEquals(10, resolucao.concluidas());
    assertEquals(5.5 * 3600, resolucao.medianaSegundos(), 0.5);
    assertEquals(9.1 * 3600, resolucao.p90Segundos(), 0.5);
  }

  @Test
  void shouldReturnNullPercentilesWhenNothingWasCompletedInTheWindow() {
    // Arrange
    aberta("A_FAZER", null, Duration.ofHours(1));

    // Act
    final var resolucao = repositorio.resolucao(inicio, fim);

    // Assert
    assertEquals(0, resolucao.concluidas());
    assertNull(resolucao.medianaSegundos());
    assertNull(resolucao.p90Segundos());
  }

  // ── backlog ──────────────────────────────────────────────────────────────

  @Test
  void shouldSplitBacklogByAgeAndSumToTheOpenTotal() {
    // Arrange
    aberta("A_FAZER", null, Duration.ofHours(2));
    final UUID meio = aberta("EM_ANDAMENTO", "ALTA", Duration.ofDays(2));
    aberta("EM_VALIDACAO", "BAIXA", Duration.ofDays(5));
    atribuir(meio, usuario("Resp"));
    solicitacao("REPARO", "CANCELADA", null, modelo, agora.minus(Duration.ofDays(9)), null);

    // Act
    final var backlog = repositorio.backlog(agora);

    // Assert
    assertEquals(3, backlog.abertas());
    assertEquals(1, backlog.menosDeUmDia());
    assertEquals(1, backlog.umATresDias());
    assertEquals(1, backlog.maisDeTresDias());
    assertEquals(
        backlog.abertas(),
        backlog.menosDeUmDia() + backlog.umATresDias() + backlog.maisDeTresDias());
    assertEquals(2, backlog.semResponsavel());
  }

  // ── etapas e devolução ───────────────────────────────────────────────────

  private TempoDeEtapa etapa(final List<TempoDeEtapa> etapas, final String nome) {
    return etapas.stream().filter(e -> e.etapa().equals(nome)).findFirst().orElse(null);
  }

  @Test
  void shouldMeasureTimeInEachStage() {
    // Arrange: A Fazer 2 h, Em andamento 5 h, Em validação 1 h
    final Instant criada = agora.minus(Duration.ofDays(2));
    final UUID id = solicitacao("REPARO", "CONCLUIDA", "ALTA", modelo, criada, null);
    mudanca(id, "A_FAZER", "EM_ANDAMENTO", criada.plus(Duration.ofHours(2)));
    mudanca(id, "EM_ANDAMENTO", "EM_VALIDACAO", criada.plus(Duration.ofHours(7)));
    mudanca(id, "EM_VALIDACAO", "CONCLUIDA", criada.plus(Duration.ofHours(8)));

    // Act
    final var etapas = repositorio.tempoPorEtapa(inicio, fim);

    // Assert
    assertEquals(2 * 3600.0, etapa(etapas, "A_FAZER").mediaSegundos(), 0.5);
    assertEquals(5 * 3600.0, etapa(etapas, "EM_ANDAMENTO").mediaSegundos(), 0.5);
    assertEquals(1 * 3600.0, etapa(etapas, "EM_VALIDACAO").medianaSegundos(), 0.5);
    assertEquals(1, etapa(etapas, "EM_ANDAMENTO").amostras());
  }

  @Test
  void shouldSumReturnedStagePassesAndCountOneReturn() {
    // Arrange: Em andamento 3 h, devolvida, Em andamento mais 2 h
    final Instant criada = agora.minus(Duration.ofDays(2));
    final UUID id = solicitacao("REPARO", "EM_VALIDACAO", "ALTA", modelo, criada, null);
    mudanca(id, "A_FAZER", "EM_ANDAMENTO", criada.plus(Duration.ofHours(1)));
    mudanca(id, "EM_ANDAMENTO", "EM_VALIDACAO", criada.plus(Duration.ofHours(4)));
    mudanca(id, "EM_VALIDACAO", "EM_ANDAMENTO", criada.plus(Duration.ofHours(5)));
    mudanca(id, "EM_ANDAMENTO", "EM_VALIDACAO", criada.plus(Duration.ofHours(7)));

    // Act
    final var etapas = repositorio.tempoPorEtapa(inicio, fim);
    final var devolucoes = repositorio.devolucoes(inicio, fim);

    // Assert
    assertEquals(5 * 3600.0, etapa(etapas, "EM_ANDAMENTO").mediaSegundos(), 0.5);
    assertEquals(2, devolucoes.enviadasParaValidacao());
    assertEquals(1, devolucoes.devolucoes());
  }

  @Test
  void shouldNotCountCancelledBeforeStartingAsWaitingToStart() {
    // Arrange
    final Instant criada = agora.minus(Duration.ofDays(1));
    final UUID id = solicitacao("REPARO", "CANCELADA", null, modelo, criada, null);
    mudanca(id, "A_FAZER", "CANCELADA", criada.plus(Duration.ofHours(3)));

    // Act
    final var etapas = repositorio.tempoPorEtapa(inicio, fim);

    // Assert
    assertNull(etapa(etapas, "A_FAZER"));
  }

  // ── máquina, reincidência e carga ────────────────────────────────────────

  @Test
  void shouldReportOpenLateAndAverageResolutionPerMachineLeavingCreationOut() {
    // Arrange
    final UUID vick = modelo("VICK", "MOD-V");
    solicitacao("REPARO", "EM_ANDAMENTO", "URGENTE", modelo, agora.minus(Duration.ofDays(2)), null);
    solicitacao("REPARO", "A_FAZER", null, modelo, agora.minus(Duration.ofHours(1)), null);
    concluida("MEDIA", Duration.ofHours(10));
    solicitacao("INSPECAO", "EM_ANDAMENTO", "BAIXA", vick, agora.minus(Duration.ofHours(1)), null);
    final Instant criada = agora.minus(Duration.ofDays(3));
    solicitacao("CRIACAO", "CONCLUIDA", "BAIXA", vick, criada, criada.plus(Duration.ofHours(100)));

    // Act
    final var maquinas = repositorio.porMaquina(inicio, fim, agora);

    // Assert
    assertEquals(2, maquinas.size());
    final var fbox = maquinas.get(0);
    assertEquals("FBOX", fbox.maquina());
    assertEquals(2, fbox.abertas());
    assertEquals(1, fbox.emAtraso());
    assertEquals(10 * 3600.0, fbox.tempoMedioResolucaoSegundos(), 0.5);
    final var vickLinha = maquinas.get(1);
    assertEquals(1, vickLinha.abertas());
    assertNull(vickLinha.tempoMedioResolucaoSegundos());
  }

  @Test
  void shouldListOnlyModelsWithThreeOrMoreRepairsAndIgnoreCreation() {
    // Arrange
    final UUID outro = modelo("FBOX", "MOD-B");
    for (int i = 0; i < 3; i++) {
      solicitacao("REPARO", "A_FAZER", null, modelo, agora.minus(Duration.ofDays(i + 1)), null);
    }
    for (int i = 0; i < 2; i++) {
      solicitacao("REPARO", "A_FAZER", null, outro, agora.minus(Duration.ofDays(i + 1)), null);
    }
    final Instant criada = agora.minus(Duration.ofDays(1));
    solicitacao("CRIACAO", "CONCLUIDA", null, outro, criada, criada.plusSeconds(60));

    // Act
    final var reincidentes = repositorio.reincidentes(inicio, fim, 3);

    // Assert
    assertEquals(1, reincidentes.size());
    assertEquals(modelo, reincidentes.get(0).modeloId());
    assertEquals(3, reincidentes.get(0).solicitacoes());
  }

  @Test
  void shouldCountSolicitacaoWithTwoResponsiblesForBothAndReportDistinctTotal() {
    // Arrange
    final UUID ana = usuario("Ana");
    final UUID beto = usuario("Beto");
    final UUID compartilhada = aberta("EM_ANDAMENTO", "ALTA", Duration.ofHours(5));
    final UUID soDaAna = aberta("EM_ANDAMENTO", "ALTA", Duration.ofHours(6));
    atribuir(compartilhada, ana);
    atribuir(compartilhada, beto);
    atribuir(soDaAna, ana);

    // Act
    final var carga = repositorio.cargaPorResponsavel();
    final long distintas = repositorio.abertasComResponsavel();

    // Assert
    assertEquals(2, carga.size());
    assertTrue(carga.get(0).nome().startsWith("Ana"));
    assertEquals(2, carga.get(0).abertas());
    assertEquals(1, carga.get(1).abertas());
    assertEquals(2, distintas);
  }
}
