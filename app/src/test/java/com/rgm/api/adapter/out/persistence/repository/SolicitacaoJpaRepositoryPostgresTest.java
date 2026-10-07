package com.rgm.api.adapter.out.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.rgm.api.adapter.out.persistence.entity.SolicitacaoJpaEntity;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Exercita o SQL nativo das consultas de solicitacao em PostgreSQL real, com as migracoes. */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class SolicitacaoJpaRepositoryPostgresTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("rgm_test");

  private static final Pageable PRIMEIRA_PAGINA =
      PageRequest.of(0, 50, Sort.by(Sort.Direction.ASC, "criada_em"));

  @Autowired private SolicitacaoJpaRepository repository;
  @Autowired private JdbcTemplate jdbc;

  private Instant agora;
  private UUID usuarioId;
  private UUID modeloId;

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
    agora = Instant.now();
    usuarioId = criarUsuario();
    modeloId = criarModelo("FBOX");
  }

  private UUID criarUsuario() {
    final UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO usuarios (id, nome, perfil) VALUES (?, ?, 'OPERADOR')", id, "Usuario " + id);
    return id;
  }

  private UUID criarModelo(final String maquina) {
    final UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO modelos (id, codigo, descricao, maquina) VALUES (?, ?, 'Modelo', ?)",
        id,
        "M-" + id.toString().substring(0, 8),
        maquina);
    return id;
  }

  private SolicitacaoJpaEntity persistir(
      final StatusSolicitacao status, final Instant criadaEm, final Instant encerradaEm) {
    return persistir(status, criadaEm, encerradaEm, usuarioId, modeloId, null);
  }

  private SolicitacaoJpaEntity persistir(
      final StatusSolicitacao status,
      final Instant criadaEm,
      final Instant encerradaEm,
      final UUID abertaPor,
      final UUID modelo,
      final PrioridadeSolicitacao prioridade) {
    final SolicitacaoJpaEntity s = new SolicitacaoJpaEntity();
    s.setId(UUID.randomUUID());
    s.setTitulo("Trocar peca");
    s.setTipo(TipoSolicitacao.REPARO);
    s.setStatus(status);
    s.setPrioridade(prioridade);
    s.setModeloId(modelo);
    s.setAbertaPorUsuarioId(abertaPor);
    s.setCriadaEm(criadaEm);
    s.setAtualizadaEm(criadaEm);
    if (status == StatusSolicitacao.CONCLUIDA) {
      s.setConcluidaEm(encerradaEm);
    }
    if (status == StatusSolicitacao.CANCELADA) {
      s.setCanceladaEm(encerradaEm);
    }
    return repository.saveAndFlush(s);
  }

  private void persistirUmaDeCadaStatus() {
    final Instant criadaEm = agora.minus(Duration.ofDays(5));
    for (final StatusSolicitacao status : StatusSolicitacao.values()) {
      persistir(status, criadaEm, agora.minus(Duration.ofDays(1)));
    }
  }

  private Page<SolicitacaoJpaEntity> filtrar(
      final StatusSolicitacao status,
      final PrioridadeSolicitacao prioridade,
      final Instant encerradaInicio,
      final Instant encerradaFim,
      final UUID abertaPor,
      final boolean emAberto,
      final Pageable pageable) {
    return repository.findByFilters(
        status != null ? status.name() : null,
        modeloId,
        null,
        prioridade != null ? prioridade.name() : null,
        null,
        null,
        encerradaInicio,
        encerradaFim,
        abertaPor,
        null,
        null,
        null,
        emAberto,
        null,
        pageable);
  }

  private Page<SolicitacaoJpaEntity> filtrarVisivelPara(
      final UUID visivelPara, final UUID abertaPor, final UUID responsavel) {
    return repository.findByFilters(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        abertaPor,
        responsavel,
        null,
        null,
        false,
        visivelPara,
        PRIMEIRA_PAGINA);
  }

  private SolicitacaoJpaEntity persistirAbertaPor(final UUID abertaPor) {
    return persistir(StatusSolicitacao.A_FAZER, agora, null, abertaPor, modeloId, null);
  }

  private void atribuir(
      final SolicitacaoJpaEntity solicitacao, final UUID responsavel, final Instant removidoEm) {
    jdbc.update(
        "INSERT INTO solicitacao_atribuicoes "
            + "(id, solicitacao_id, usuario_id, atribuido_por_usuario_id, removido_em) "
            + "VALUES (?, ?, ?, ?, ?)",
        UUID.randomUUID(),
        solicitacao.getId(),
        responsavel,
        usuarioId,
        removidoEm != null ? java.sql.Timestamp.from(removidoEm) : null);
  }

  private static List<UUID> ids(final Page<SolicitacaoJpaEntity> page) {
    return page.getContent().stream().map(SolicitacaoJpaEntity::getId).sorted().toList();
  }

  @Test
  void shouldDevolverTodasWhenVisibilidadeVemVazia() {
    // Arrange
    final int total = 3;
    for (int i = 0; i < total; i++) {
      persistirAbertaPor(criarUsuario());
    }

    // Act
    final var page = filtrarVisivelPara(null, null, null);

    // Assert
    assertEquals(total, page.getTotalElements());
  }

  @Test
  void shouldDevolverAsQueAbriuEAsDeQueEResponsavelWhenVisibilidadeEDoOperador() {
    // Arrange
    final UUID operador = criarUsuario();
    final UUID outro = criarUsuario();
    final int abertasPorEle = 3;
    final int atribuidasAEle = 2;
    final int alheias = 4;
    final List<UUID> esperadas = new java.util.ArrayList<>();
    for (int i = 0; i < abertasPorEle; i++) {
      esperadas.add(persistirAbertaPor(operador).getId());
    }
    for (int i = 0; i < atribuidasAEle; i++) {
      final SolicitacaoJpaEntity s = persistirAbertaPor(outro);
      atribuir(s, operador, null);
      esperadas.add(s.getId());
    }
    for (int i = 0; i < alheias; i++) {
      atribuir(persistirAbertaPor(outro), outro, null);
    }

    // Act
    final var page = filtrarVisivelPara(operador, null, null);

    // Assert
    assertEquals(abertasPorEle + atribuidasAEle, page.getTotalElements());
    assertEquals(esperadas.stream().sorted().toList(), ids(page));
  }

  @Test
  void shouldContarUmaVezWhenOperadorAbriuEEResponsavelDaMesmaSolicitacao() {
    // Arrange
    final UUID operador = criarUsuario();
    final SolicitacaoJpaEntity s = persistirAbertaPor(operador);
    atribuir(s, operador, null);

    // Act
    final var page = filtrarVisivelPara(operador, null, null);

    // Assert
    assertEquals(List.of(s.getId()), ids(page));
    assertEquals(1, page.getTotalElements());
  }

  @Test
  void shouldDeixarDeForaWhenOperadorFoiRemovidoDeResponsavel() {
    // Arrange
    final UUID operador = criarUsuario();
    atribuir(persistirAbertaPor(criarUsuario()), operador, agora);

    // Act
    final var page = filtrarVisivelPara(operador, null, null);

    // Assert
    assertEquals(0, page.getTotalElements());
  }

  @Test
  void shouldDevolverListaVaziaWhenOperadorFiltraPorOutroUsuarioSemRelacaoComEle() {
    // Arrange
    final UUID operador = criarUsuario();
    final UUID outro = criarUsuario();
    persistirAbertaPor(operador);
    for (int i = 0; i < 4; i++) {
      persistirAbertaPor(outro);
    }

    // Act
    final var page = filtrarVisivelPara(operador, outro, null);

    // Assert
    assertEquals(0, page.getTotalElements());
  }

  @Test
  void shouldRestringirAsQueAbriuWhenOperadorFiltraPorSiMesmo() {
    // Arrange
    final UUID operador = criarUsuario();
    final int abertasPorEle = 3;
    final List<UUID> esperadas = new java.util.ArrayList<>();
    for (int i = 0; i < abertasPorEle; i++) {
      esperadas.add(persistirAbertaPor(operador).getId());
    }
    for (int i = 0; i < 2; i++) {
      atribuir(persistirAbertaPor(criarUsuario()), operador, null);
    }

    // Act
    final var page = filtrarVisivelPara(operador, operador, null);

    // Assert
    assertEquals(esperadas.stream().sorted().toList(), ids(page));
  }

  @Test
  void shouldRestringirAsDeQueEResponsavelWhenOperadorFiltraPorResponsavel() {
    // Arrange
    final UUID operador = criarUsuario();
    persistirAbertaPor(operador);
    final SolicitacaoJpaEntity atribuida = persistirAbertaPor(criarUsuario());
    atribuir(atribuida, operador, null);

    // Act
    final var page = filtrarVisivelPara(operador, null, operador);

    // Assert
    assertEquals(List.of(atribuida.getId()), ids(page));
  }

  @Test
  void shouldDevolverSoStatusNaoEncerradosWhenEmAbertoVemLigado() {
    // Arrange
    persistirUmaDeCadaStatus();
    final List<StatusSolicitacao> esperados =
        List.of(
            StatusSolicitacao.A_FAZER,
            StatusSolicitacao.EM_ANDAMENTO,
            StatusSolicitacao.EM_VALIDACAO);

    // Act
    final var page = filtrar(null, null, null, null, null, true, PRIMEIRA_PAGINA);

    // Assert
    assertEquals(esperados.size(), page.getTotalElements());
    assertEquals(
        esperados,
        page.getContent().stream().map(SolicitacaoJpaEntity::getStatus).sorted().toList());
  }

  @Test
  void shouldDevolverTodosOsStatusWhenEmAbertoVemDesligado() {
    // Arrange
    persistirUmaDeCadaStatus();

    // Act
    final var page = filtrar(null, null, null, null, null, false, PRIMEIRA_PAGINA);

    // Assert
    assertEquals(StatusSolicitacao.values().length, page.getTotalElements());
  }

  @Test
  void shouldPaginarEContarSoAsEmAbertoDeQuemAbriuWhenEmAbertoCombinaComAutor() {
    // Arrange
    final int emAbertoDoAutor = 5;
    final int tamanhoDaPagina = 2;
    final Instant criadaEm = agora.minus(Duration.ofDays(3));
    for (int i = 0; i < emAbertoDoAutor; i++) {
      persistir(StatusSolicitacao.A_FAZER, criadaEm.plusSeconds(i), null);
    }
    persistir(StatusSolicitacao.CONCLUIDA, criadaEm, agora);
    persistir(StatusSolicitacao.EM_ANDAMENTO, criadaEm, null, criarUsuario(), modeloId, null);

    // Act
    final var page =
        filtrar(
            null,
            null,
            null,
            null,
            usuarioId,
            true,
            PageRequest.of(0, tamanhoDaPagina, Sort.by(Sort.Direction.ASC, "criada_em")));

    // Assert
    assertEquals(tamanhoDaPagina, page.getContent().size());
    assertEquals(emAbertoDoAutor, page.getTotalElements());
  }

  @Test
  void shouldContarSoAsEmAbertoDaPrioridadeWhenEmAbertoCombinaComPrioridade() {
    // Arrange
    final Instant criadaEm = agora.minus(Duration.ofDays(3));
    final int emAbertoComAlta = 3;
    for (int i = 0; i < emAbertoComAlta; i++) {
      persistir(
          StatusSolicitacao.EM_ANDAMENTO,
          criadaEm,
          null,
          usuarioId,
          modeloId,
          PrioridadeSolicitacao.ALTA);
    }
    persistir(
        StatusSolicitacao.CONCLUIDA,
        criadaEm,
        agora,
        usuarioId,
        modeloId,
        PrioridadeSolicitacao.ALTA);
    persistir(
        StatusSolicitacao.EM_ANDAMENTO,
        criadaEm,
        null,
        usuarioId,
        modeloId,
        PrioridadeSolicitacao.BAIXA);

    // Act
    final var page =
        filtrar(null, PrioridadeSolicitacao.ALTA, null, null, null, true, PRIMEIRA_PAGINA);

    // Assert
    assertEquals(emAbertoComAlta, page.getTotalElements());
  }

  @Test
  void shouldDevolverListaVaziaWhenEmAbertoCombinaComStatusEncerrado() {
    // Arrange
    persistirUmaDeCadaStatus();

    // Act
    final var page =
        filtrar(StatusSolicitacao.CONCLUIDA, null, null, null, null, true, PRIMEIRA_PAGINA);

    // Assert
    assertEquals(0, page.getTotalElements());
  }

  @Test
  void shouldFiltrarCanceladasPelaDataDeCancelamentoWhenFiltroEPorEncerramento() {
    // Arrange
    final Instant criadaEm = agora.minus(Duration.ofDays(90));
    final var recente =
        persistir(StatusSolicitacao.CANCELADA, criadaEm, agora.minus(Duration.ofDays(10)));
    persistir(StatusSolicitacao.CANCELADA, criadaEm, agora.minus(Duration.ofDays(60)));

    // Act
    final var page =
        filtrar(
            StatusSolicitacao.CANCELADA,
            null,
            agora.minus(Duration.ofDays(30)),
            agora,
            null,
            false,
            PRIMEIRA_PAGINA);

    // Assert
    assertEquals(
        List.of(recente.getId()),
        page.getContent().stream().map(SolicitacaoJpaEntity::getId).toList());
  }

  @Test
  void shouldFiltrarConcluidasPelaDataDeConclusaoWhenFiltroEPorEncerramento() {
    // Arrange
    final Instant criadaEm = agora.minus(Duration.ofDays(90));
    final var recente =
        persistir(StatusSolicitacao.CONCLUIDA, criadaEm, agora.minus(Duration.ofDays(10)));
    persistir(StatusSolicitacao.CONCLUIDA, criadaEm, agora.minus(Duration.ofDays(60)));

    // Act
    final var page =
        filtrar(
            StatusSolicitacao.CONCLUIDA,
            null,
            agora.minus(Duration.ofDays(30)),
            agora,
            null,
            false,
            PRIMEIRA_PAGINA);

    // Assert
    assertEquals(
        List.of(recente.getId()),
        page.getContent().stream().map(SolicitacaoJpaEntity::getId).toList());
  }

  @Test
  void shouldDeixarDeForaAsEmAbertoWhenFiltroEPorEncerramento() {
    // Arrange
    persistirUmaDeCadaStatus();

    // Act
    final var page =
        filtrar(null, null, agora.minus(Duration.ofDays(30)), agora, null, false, PRIMEIRA_PAGINA);

    // Assert
    assertEquals(
        List.of(StatusSolicitacao.CONCLUIDA, StatusSolicitacao.CANCELADA),
        page.getContent().stream().map(SolicitacaoJpaEntity::getStatus).sorted().toList());
  }

  private void persistirHistoricoDeQuatroSolicitacoes() {
    final Instant dia1 = agora.minus(Duration.ofDays(40));
    persistir(StatusSolicitacao.CONCLUIDA, dia1, dia1.plus(Duration.ofDays(2)));
    persistir(
        StatusSolicitacao.CANCELADA,
        dia1.plus(Duration.ofDays(10)),
        dia1.plus(Duration.ofDays(12)));
    persistir(
        StatusSolicitacao.CONCLUIDA,
        dia1.plus(Duration.ofDays(20)),
        dia1.plus(Duration.ofDays(24)));
    persistir(StatusSolicitacao.A_FAZER, dia1.plus(Duration.ofDays(30)), null);
  }

  @Test
  void shouldResumirContagensEMediaDeResolucaoWhenResumePorModelo() {
    // Arrange
    persistirHistoricoDeQuatroSolicitacoes();
    final double mediaEsperada = Duration.ofDays(3).getSeconds();

    // Act
    final Object[] row = repository.resumirPorModelo(modeloId).get(0);

    // Assert
    assertEquals(4L, ((Number) row[0]).longValue());
    assertEquals(1L, ((Number) row[1]).longValue());
    assertEquals(2L, ((Number) row[2]).longValue());
    assertEquals(1L, ((Number) row[3]).longValue());
    assertEquals(mediaEsperada, ((Number) row[6]).doubleValue(), 0.001);
  }

  @Test
  void shouldDarOMesmoIntervaloDoRankingWhenResumePorModelo() {
    // Arrange
    persistirHistoricoDeQuatroSolicitacoes();
    final Object[] ranking =
        repository
            .findMetricasPorModelo(
                PageRequest.of(0, 50, Sort.by(Sort.Direction.ASC, "tempo_medio_resolucao")))
            .getContent()
            .stream()
            .filter(linha -> modeloId.equals(linha[0]))
            .findFirst()
            .orElseThrow();

    // Act
    final Object[] row = repository.resumirPorModelo(modeloId).get(0);
    final double intervalo =
        Duration.between((Instant) row[4], (Instant) row[5]).toMillis()
            / 1000.0
            / (((Number) row[0]).longValue() - 1);

    // Assert
    assertEquals(((Number) ranking[3]).doubleValue(), intervalo, 0.001);
    assertEquals(((Number) ranking[2]).doubleValue(), ((Number) row[6]).doubleValue(), 0.001);
  }

  @Test
  void shouldDevolverTemposVaziosWhenModeloNaoTemSolicitacao() {
    // Arrange
    final UUID modeloSemSolicitacao = criarModelo("DISA");

    // Act
    final Object[] row = repository.resumirPorModelo(modeloSemSolicitacao).get(0);

    // Assert
    assertEquals(0L, ((Number) row[0]).longValue());
    assertNull(row[4]);
    assertNull(row[6]);
  }
}
