package com.rgm.api.adapter.out.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.rgm.api.adapter.out.persistence.entity.SolicitacaoJpaEntity;
import com.rgm.api.core.domain.model.enums.CriterioOrdenacaoSolicitacao;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.OrdenacaoSolicitacao;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
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

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class SolicitacaoJpaRepositoryPostgresTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("rgm_test");

  private static final Pageable PRIMEIRA_PAGINA = PageRequest.of(0, 50);

  private static final String CRITERIO_PADRAO = CriterioOrdenacaoSolicitacao.CRIADA_EM.name();

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
        null,
        CRITERIO_PADRAO,
        true,
        pageable);
  }

  private Page<SolicitacaoJpaEntity> listarOrdenado(
      final CriterioOrdenacaoSolicitacao criterio,
      final boolean ascendente,
      final Pageable pageable) {
    return repository.findByFilters(
        null,
        modeloId,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        false,
        null,
        null,
        criterio.name(),
        ascendente,
        pageable);
  }

  private List<UUID> idsNaOrdem(final Page<SolicitacaoJpaEntity> page) {
    return page.getContent().stream().map(SolicitacaoJpaEntity::getId).toList();
  }

  private UUID persistirComPrioridade(
      final PrioridadeSolicitacao prioridade, final Duration idade) {
    return persistir(
            StatusSolicitacao.A_FAZER, agora.minus(idade), null, usuarioId, modeloId, prioridade)
        .getId();
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
        null,
        CRITERIO_PADRAO,
        true,
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
  void shouldTrazerAMaisVencidaPrimeiroEASemPrioridadeNoFimWhenPrazoLimiteECrescente() {
    // Arrange
    final UUID baixa = persistirComPrioridade(PrioridadeSolicitacao.BAIXA, Duration.ofHours(1));
    final UUID urgente =
        persistirComPrioridade(PrioridadeSolicitacao.URGENTE, Duration.ofHours(10));
    final UUID semPrioridade = persistirComPrioridade(null, Duration.ofHours(20));
    final UUID media = persistirComPrioridade(PrioridadeSolicitacao.MEDIA, Duration.ofHours(2));

    // Act
    final var page =
        listarOrdenado(CriterioOrdenacaoSolicitacao.PRAZO_LIMITE, true, PRIMEIRA_PAGINA);

    // Assert
    assertEquals(List.of(urgente, media, baixa, semPrioridade), idsNaOrdem(page));
  }

  @Test
  void shouldManterASemPrioridadeNoFimWhenPrazoLimiteEDecrescente() {
    // Arrange
    final UUID baixa = persistirComPrioridade(PrioridadeSolicitacao.BAIXA, Duration.ofHours(1));
    final UUID urgente =
        persistirComPrioridade(PrioridadeSolicitacao.URGENTE, Duration.ofHours(10));
    final UUID semPrioridade = persistirComPrioridade(null, Duration.ofHours(20));
    final UUID media = persistirComPrioridade(PrioridadeSolicitacao.MEDIA, Duration.ofHours(2));

    // Act
    final var page =
        listarOrdenado(CriterioOrdenacaoSolicitacao.PRAZO_LIMITE, false, PRIMEIRA_PAGINA);

    // Assert
    assertEquals(List.of(baixa, media, urgente, semPrioridade), idsNaOrdem(page));
  }

  @Test
  void shouldTrazerAMaisNovaPrimeiroWhenCriacaoEDecrescente() {
    // Arrange
    final UUID antiga = persistirComPrioridade(null, Duration.ofDays(3));
    final UUID nova = persistirComPrioridade(null, Duration.ofHours(1));
    final UUID intermediaria = persistirComPrioridade(null, Duration.ofDays(1));

    // Act
    final var page = listarOrdenado(CriterioOrdenacaoSolicitacao.CRIADA_EM, false, PRIMEIRA_PAGINA);

    // Assert
    assertEquals(List.of(nova, intermediaria, antiga), idsNaOrdem(page));
  }

  @Test
  void shouldTrazerAMaisAntigaPrimeiroWhenOrdenacaoEOPadrao() {
    // Arrange
    final UUID nova = persistirComPrioridade(null, Duration.ofHours(1));
    final UUID antiga = persistirComPrioridade(null, Duration.ofDays(3));
    final UUID intermediaria = persistirComPrioridade(null, Duration.ofDays(1));

    // Act
    final var page =
        listarOrdenado(
            OrdenacaoSolicitacao.PADRAO.criterio(),
            OrdenacaoSolicitacao.PADRAO.ascendente(),
            PRIMEIRA_PAGINA);

    // Assert
    assertEquals(List.of(antiga, intermediaria, nova), idsNaOrdem(page));
  }

  @Test
  void shouldTrazerUrgenteAntesDeBaixaWhenPrioridadeECrescente() {
    // Arrange
    final UUID baixa = persistirComPrioridade(PrioridadeSolicitacao.BAIXA, Duration.ofDays(4));
    final UUID semPrioridade = persistirComPrioridade(null, Duration.ofDays(3));
    final UUID urgente = persistirComPrioridade(PrioridadeSolicitacao.URGENTE, Duration.ofDays(2));
    final UUID media = persistirComPrioridade(PrioridadeSolicitacao.MEDIA, Duration.ofDays(1));
    final UUID alta = persistirComPrioridade(PrioridadeSolicitacao.ALTA, Duration.ofHours(1));

    // Act
    final var page = listarOrdenado(CriterioOrdenacaoSolicitacao.PRIORIDADE, true, PRIMEIRA_PAGINA);

    // Assert
    assertEquals(List.of(urgente, alta, media, baixa, semPrioridade), idsNaOrdem(page));
  }

  @Test
  void shouldTrazerBaixaAntesDeUrgenteEASemPrioridadeNoFimWhenPrioridadeEDecrescente() {
    // Arrange
    final UUID baixa = persistirComPrioridade(PrioridadeSolicitacao.BAIXA, Duration.ofDays(4));
    final UUID semPrioridade = persistirComPrioridade(null, Duration.ofDays(3));
    final UUID urgente = persistirComPrioridade(PrioridadeSolicitacao.URGENTE, Duration.ofDays(2));

    // Act
    final var page =
        listarOrdenado(CriterioOrdenacaoSolicitacao.PRIORIDADE, false, PRIMEIRA_PAGINA);

    // Assert
    assertEquals(List.of(baixa, urgente, semPrioridade), idsNaOrdem(page));
  }

  @Test
  void shouldNaoRepetirNemPerderItemWhenPaginasConsecutivasTemEmpateNoCriterio() {
    // Arrange
    final int total = 5;
    final Duration idade = Duration.ofHours(3);
    final Instant criadaEm = agora.minus(idade);
    for (int i = 0; i < total; i++) {
      persistir(
          StatusSolicitacao.A_FAZER,
          criadaEm,
          null,
          usuarioId,
          modeloId,
          PrioridadeSolicitacao.ALTA);
    }
    final List<UUID> esperados =
        idsNaOrdem(listarOrdenado(CriterioOrdenacaoSolicitacao.PRIORIDADE, true, PRIMEIRA_PAGINA));

    // Act
    final List<UUID> paginado = new java.util.ArrayList<>();
    for (int pagina = 0; pagina < 3; pagina++) {
      paginado.addAll(
          idsNaOrdem(
              listarOrdenado(
                  CriterioOrdenacaoSolicitacao.PRIORIDADE, true, PageRequest.of(pagina, 2))));
    }

    // Assert
    assertEquals(total, paginado.stream().distinct().count());
    assertEquals(esperados, paginado);
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
        filtrar(null, null, null, null, usuarioId, true, PageRequest.of(0, tamanhoDaPagina));

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
            / (((Number) row[7]).longValue() - 1);

    // Assert
    assertEquals(((Number) ranking[3]).doubleValue(), intervalo, 0.001);
    assertEquals(((Number) ranking[2]).doubleValue(), ((Number) row[6]).doubleValue(), 0.001);
  }

  private static final String FUSO = "America/Sao_Paulo";
  private static final Instant JANELA_INICIO = Instant.parse("2026-09-01T03:00:00Z");
  private static final Instant JANELA_FIM = Instant.parse("2026-10-01T03:00:00Z");

  private List<Object[]> serie(final String unidade, final UUID modelo) {
    return repository.agregarSerie(JANELA_INICIO, JANELA_FIM, modelo, unidade, FUSO);
  }

  private void persistirAberta(final String criadaEm) {
    persistir(StatusSolicitacao.A_FAZER, Instant.parse(criadaEm), null);
  }

  @Test
  void shouldCairNoProprioDiaLocalWhenSolicitacaoFoiCriadaAs22hDeBrasilia() {
    // Arrange
    persistirAberta("2026-09-10T01:00:00Z");

    // Act
    final List<Object[]> linhas = serie("day", null);

    // Assert
    assertEquals(1, linhas.size());
    assertEquals("2026-09-09", linhas.get(0)[0].toString());
  }

  @Test
  void shouldSepararODiaWhenSolicitacoesEstaoEm23h59E00h00Locais() {
    // Arrange
    persistirAberta("2026-09-10T02:59:00Z");
    persistirAberta("2026-09-10T03:00:00Z");

    // Act
    final List<Object[]> linhas = serie("day", null);

    // Assert
    assertEquals(2, linhas.size());
    assertEquals("2026-09-09", linhas.get(0)[0].toString());
    assertEquals(1L, ((Number) linhas.get(0)[1]).longValue());
    assertEquals("2026-09-10", linhas.get(1)[0].toString());
    assertEquals(1L, ((Number) linhas.get(1)[1]).longValue());
  }

  @Test
  void shouldContarPorStatusEMediaDeResolucaoWhenAgregaODia() {
    // Arrange
    final Instant criadaEm = Instant.parse("2026-09-10T13:00:00Z");
    persistir(StatusSolicitacao.CONCLUIDA, criadaEm, criadaEm.plus(Duration.ofHours(1)));
    persistir(StatusSolicitacao.CONCLUIDA, criadaEm, criadaEm.plus(Duration.ofHours(3)));
    persistir(StatusSolicitacao.CANCELADA, criadaEm, criadaEm.plus(Duration.ofHours(2)));
    persistir(StatusSolicitacao.EM_VALIDACAO, criadaEm, null);
    persistir(StatusSolicitacao.A_FAZER, criadaEm, null);

    // Act
    final Object[] linha = serie("day", null).get(0);

    // Assert
    assertEquals(5L, ((Number) linha[1]).longValue());
    assertEquals(2L, ((Number) linha[2]).longValue());
    assertEquals(2L, ((Number) linha[3]).longValue());
    assertEquals(1L, ((Number) linha[4]).longValue());
    assertEquals(7200.0, ((Number) linha[5]).doubleValue(), 0.001);
  }

  @Test
  void shouldDeixarAMediaVaziaWhenDiaNaoTemConcluida() {
    // Arrange
    persistirAberta("2026-09-10T13:00:00Z");

    // Act
    final Object[] linha = serie("day", null).get(0);

    // Assert
    assertNull(linha[5]);
  }

  @Test
  void shouldAgruparPelaDataDeCriacaoWhenConcluidaEmOutroDia() {
    // Arrange
    final Instant criadaEm = Instant.parse("2026-09-10T13:00:00Z");
    persistir(StatusSolicitacao.CONCLUIDA, criadaEm, criadaEm.plus(Duration.ofDays(2)));

    // Act
    final List<Object[]> linhas = serie("day", null);

    // Assert
    assertEquals(1, linhas.size());
    assertEquals("2026-09-10", linhas.get(0)[0].toString());
  }

  @Test
  void shouldFiltrarPeloModeloNaConsultaWhenModeloVemPreenchido() {
    // Arrange
    final UUID outroModelo = criarModelo("DISA");
    final Instant criadaEm = Instant.parse("2026-09-10T13:00:00Z");
    persistir(StatusSolicitacao.A_FAZER, criadaEm, null);
    persistir(StatusSolicitacao.A_FAZER, criadaEm, null, usuarioId, outroModelo, null);
    persistir(StatusSolicitacao.A_FAZER, criadaEm, null, usuarioId, outroModelo, null);

    // Act
    final Object[] linha = serie("day", outroModelo).get(0);

    // Assert
    assertEquals(2L, ((Number) linha[1]).longValue());
  }

  @Test
  void shouldAgruparDeSegundaADomingoWhenUnidadeESemana() {
    // Arrange
    persistirAberta("2026-09-14T02:59:00Z");
    persistirAberta("2026-09-14T03:00:00Z");
    persistirAberta("2026-09-20T14:00:00Z");

    // Act
    final List<Object[]> linhas = serie("week", null);

    // Assert
    assertEquals(2, linhas.size());
    assertEquals("2026-09-07", linhas.get(0)[0].toString());
    assertEquals(1L, ((Number) linhas.get(0)[1]).longValue());
    assertEquals("2026-09-14", linhas.get(1)[0].toString());
    assertEquals(2L, ((Number) linhas.get(1)[1]).longValue());
  }

  @Test
  void shouldLimitarAJanelaWhenInicioEInclusivoEFimEExclusivo() {
    // Arrange
    persistirAberta("2026-09-01T02:59:00Z");
    persistirAberta("2026-09-01T03:00:00Z");
    persistirAberta("2026-10-01T02:59:00Z");
    persistirAberta("2026-10-01T03:00:00Z");

    // Act
    final long total =
        serie("day", null).stream().mapToLong(l -> ((Number) l[1]).longValue()).sum();

    // Assert
    assertEquals(2L, total);
  }

  private static final LocalDate PAINEL_PRIMEIRO_DIA = LocalDate.of(2026, 9, 7);
  private static final LocalDate PAINEL_FIM_DIA = LocalDate.of(2026, 9, 14);
  private static final Instant PAINEL_INICIO = Instant.parse("2026-09-07T03:00:00Z");
  private static final Instant PAINEL_FIM = Instant.parse("2026-09-14T03:00:00Z");
  private static final Instant PAINEL_AGORA_DOMINGO_13_09_12H_BRASILIA =
      Instant.parse("2026-09-13T15:00:00Z");

  private Object[] totaisDoPainel(final Instant inicio, final Instant fim, final UUID modelo) {
    return repository.painelTotais(inicio, fim, modelo).get(0);
  }

  private List<Object[]> seriePainel(final String unidade, final UUID modelo) {
    return repository.painelSerie(
        PAINEL_PRIMEIRO_DIA,
        PAINEL_FIM_DIA,
        modelo,
        unidade,
        FUSO,
        PAINEL_AGORA_DOMINGO_13_09_12H_BRASILIA);
  }

  private static List<Long> coluna(final List<Object[]> linhas, final int indice) {
    return linhas.stream().map(l -> ((Number) l[indice]).longValue()).toList();
  }

  private SolicitacaoJpaEntity persistirComPrioridade(
      final StatusSolicitacao status,
      final String criadaEm,
      final String encerradaEm,
      final PrioridadeSolicitacao prioridade) {
    return persistir(
        status,
        Instant.parse(criadaEm),
        encerradaEm == null ? null : Instant.parse(encerradaEm),
        usuarioId,
        modeloId,
        prioridade);
  }

  @Test
  void shouldContarCriadasPelaDataDeCriacaoWhenJanelaTemTotais() {
    // Arrange
    persistirAberta("2026-09-07T02:59:00Z");
    persistirAberta("2026-09-07T03:00:00Z");
    persistirAberta("2026-09-13T12:00:00Z");
    persistirAberta("2026-09-14T03:00:00Z");

    // Act
    final Object[] totais = totaisDoPainel(PAINEL_INICIO, PAINEL_FIM, null);

    // Assert
    assertEquals(2L, ((Number) totais[0]).longValue());
  }

  @Test
  void shouldContarConcluidaNaJanelaDaConclusaoWhenFoiCriadaVinteDiasAntes() {
    // Arrange
    persistir(
        StatusSolicitacao.CONCLUIDA,
        Instant.parse("2026-08-18T13:00:00Z"),
        Instant.parse("2026-09-12T13:00:00Z"));

    // Act
    final Object[] totais = totaisDoPainel(PAINEL_INICIO, PAINEL_FIM, null);

    // Assert
    assertEquals(0L, ((Number) totais[0]).longValue());
    assertEquals(1L, ((Number) totais[1]).longValue());
  }

  @Test
  void shouldContarCanceladaNaJanelaDoCancelamentoWhenFoiCriadaAntes() {
    // Arrange
    persistir(
        StatusSolicitacao.CANCELADA,
        Instant.parse("2026-08-18T13:00:00Z"),
        Instant.parse("2026-09-09T13:00:00Z"));
    persistir(
        StatusSolicitacao.CANCELADA,
        Instant.parse("2026-09-08T13:00:00Z"),
        Instant.parse("2026-09-14T13:00:00Z"));

    // Act
    final Object[] totais = totaisDoPainel(PAINEL_INICIO, PAINEL_FIM, null);

    // Assert
    assertEquals(1L, ((Number) totais[2]).longValue());
  }

  @Test
  void shouldDar1800SegundosWhenResolucaoDemorouTrintaMinutos() {
    // Arrange
    final Instant criadaEm = Instant.parse("2026-09-09T13:00:00Z");
    persistir(StatusSolicitacao.CONCLUIDA, criadaEm, criadaEm.plus(Duration.ofMinutes(30)));

    // Act
    final Object[] totais = totaisDoPainel(PAINEL_INICIO, PAINEL_FIM, null);

    // Assert
    assertEquals(1800.0, ((Number) totais[3]).doubleValue(), 0.001);
  }

  @Test
  void shouldMediarSoAsConcluidasNaJanelaWhenHaConclusaoForaDela() {
    // Arrange
    final Instant criadaEm = Instant.parse("2026-09-09T13:00:00Z");
    persistir(StatusSolicitacao.CONCLUIDA, criadaEm, criadaEm.plus(Duration.ofMinutes(30)));
    persistir(StatusSolicitacao.CONCLUIDA, criadaEm, criadaEm.plus(Duration.ofMinutes(90)));
    final Instant antiga = Instant.parse("2026-08-01T13:00:00Z");
    persistir(StatusSolicitacao.CONCLUIDA, antiga, antiga.plus(Duration.ofHours(10)));

    // Act
    final Object[] totais = totaisDoPainel(PAINEL_INICIO, PAINEL_FIM, null);

    // Assert
    assertEquals(3600.0, ((Number) totais[3]).doubleValue(), 0.001);
  }

  @Test
  void shouldDeixarOTempoMedioNuloWhenNaoHaConcluidaNaJanela() {
    // Arrange
    persistirAberta("2026-09-09T13:00:00Z");

    // Act
    final Object[] totais = totaisDoPainel(PAINEL_INICIO, PAINEL_FIM, null);

    // Assert
    assertNull(totais[3]);
  }

  @Test
  void shouldTotalizarSoOModeloWhenModeloVemPreenchidoNosTotais() {
    // Arrange
    final UUID outroModelo = criarModelo("DISA");
    final Instant criadaEm = Instant.parse("2026-09-09T13:00:00Z");
    persistir(StatusSolicitacao.A_FAZER, criadaEm, null);
    persistir(StatusSolicitacao.A_FAZER, criadaEm, null, usuarioId, outroModelo, null);
    persistir(
        StatusSolicitacao.CONCLUIDA,
        criadaEm,
        criadaEm.plusSeconds(60),
        usuarioId,
        outroModelo,
        null);

    // Act
    final Object[] totais = totaisDoPainel(PAINEL_INICIO, PAINEL_FIM, outroModelo);

    // Assert
    assertEquals(2L, ((Number) totais[0]).longValue());
    assertEquals(1L, ((Number) totais[1]).longValue());
  }

  @Test
  void shouldTotalizarAJanelaAnteriorComAsMesmasRegrasWhenJanelaEDeslocada() {
    // Arrange
    final Instant criadaEm = Instant.parse("2026-08-31T13:00:00Z");
    persistir(StatusSolicitacao.CONCLUIDA, criadaEm, criadaEm.plus(Duration.ofMinutes(30)));
    persistirAberta("2026-09-09T13:00:00Z");
    final Instant inicioAnterior = PAINEL_INICIO.minus(Duration.ofDays(7));

    // Act
    final Object[] totais = totaisDoPainel(inicioAnterior, PAINEL_INICIO, null);

    // Assert
    assertEquals(1L, ((Number) totais[0]).longValue());
    assertEquals(1L, ((Number) totais[1]).longValue());
    assertEquals(1800.0, ((Number) totais[3]).doubleValue(), 0.001);
  }

  @Test
  void shouldGerarUmPontoPorDiaLocalWhenJanelaNaoTemSolicitacao() {
    // Arrange
    final String unidade = "day";

    // Act
    final List<Object[]> linhas = seriePainel(unidade, null);

    // Assert
    assertEquals(7, linhas.size());
    assertEquals("2026-09-07", linhas.get(0)[0].toString());
    assertEquals("2026-09-13", linhas.get(6)[0].toString());
    assertEquals(List.of(0L, 0L, 0L, 0L, 0L, 0L, 0L), coluna(linhas, 4));
  }

  @Test
  void shouldCairNoDiaLocalWhenCriadaAs22hNaSerieDoPainel() {
    // Arrange
    persistirAberta("2026-09-10T01:00:00Z");

    // Act
    final List<Object[]> linhas = seriePainel("day", null);

    // Assert
    assertEquals(List.of(0L, 0L, 1L, 0L, 0L, 0L, 0L), coluna(linhas, 1));
  }

  @Test
  void shouldContarConcluidaNoDiaDaConclusaoWhenSerieDoPainel() {
    // Arrange
    persistir(
        StatusSolicitacao.CONCLUIDA,
        Instant.parse("2026-08-18T13:00:00Z"),
        Instant.parse("2026-09-10T01:00:00Z"));

    // Act
    final List<Object[]> linhas = seriePainel("day", null);

    // Assert
    assertEquals(List.of(0L, 0L, 1L, 0L, 0L, 0L, 0L), coluna(linhas, 2));
    assertEquals(List.of(0L, 0L, 0L, 0L, 0L, 0L, 0L), coluna(linhas, 1));
  }

  @Test
  void shouldContarCanceladaNoDiaDoCancelamentoWhenSerieDoPainel() {
    // Arrange
    persistir(
        StatusSolicitacao.CANCELADA,
        Instant.parse("2026-08-18T13:00:00Z"),
        Instant.parse("2026-09-11T13:00:00Z"));

    // Act
    final List<Object[]> linhas = seriePainel("day", null);

    // Assert
    assertEquals(List.of(0L, 0L, 0L, 0L, 1L, 0L, 0L), coluna(linhas, 3));
  }

  @Test
  void shouldContarAbertaDesdeACriacaoAteAEncerramentoWhenAbertasAoFimDoDia() {
    // Arrange
    persistir(
        StatusSolicitacao.CONCLUIDA,
        Instant.parse("2026-09-08T13:00:00Z"),
        Instant.parse("2026-09-10T15:00:00Z"));

    // Act
    final List<Object[]> linhas = seriePainel("day", null);

    // Assert
    assertEquals(List.of(0L, 1L, 1L, 0L, 0L, 0L, 0L), coluna(linhas, 4));
  }

  @Test
  void shouldContarCanceladaComoAbertaAteOCancelamentoWhenAbertasAoFimDoDia() {
    // Arrange
    persistir(
        StatusSolicitacao.CANCELADA,
        Instant.parse("2026-09-07T13:00:00Z"),
        Instant.parse("2026-09-09T15:00:00Z"));

    // Act
    final List<Object[]> linhas = seriePainel("day", null);

    // Assert
    assertEquals(List.of(1L, 1L, 0L, 0L, 0L, 0L, 0L), coluna(linhas, 4));
  }

  @Test
  void shouldContarComoAbertaAoFimDoDiaWhenEncerradaNaMeiaNoiteDoDiaSeguinte() {
    // Arrange
    persistir(
        StatusSolicitacao.CONCLUIDA,
        Instant.parse("2026-09-08T13:00:00Z"),
        Instant.parse("2026-09-10T03:00:00Z"));

    // Act
    final List<Object[]> linhas = seriePainel("day", null);

    // Assert
    assertEquals(List.of(0L, 1L, 1L, 0L, 0L, 0L, 0L), coluna(linhas, 4));
  }

  @Test
  void shouldNaoContarComoAbertaWhenCriadaNaMeiaNoiteDoDiaSeguinte() {
    // Arrange
    persistirAberta("2026-09-10T03:00:00Z");

    // Act
    final List<Object[]> linhas = seriePainel("day", null);

    // Assert
    assertEquals(List.of(0L, 0L, 0L, 1L, 1L, 1L, 1L), coluna(linhas, 4));
  }

  @Test
  void shouldContarAtrasadaAoFimDoDiaWhenPrazoVenceAntesDoFimEEstaAberta() {
    // Arrange
    persistirComPrioridade(
        StatusSolicitacao.CONCLUIDA,
        "2026-09-08T13:00:00Z",
        "2026-09-10T15:00:00Z",
        PrioridadeSolicitacao.URGENTE);

    // Act
    final List<Object[]> linhas = seriePainel("day", null);

    // Assert
    assertEquals(List.of(0L, 1L, 1L, 0L, 0L, 0L, 0L), coluna(linhas, 5));
  }

  @Test
  void shouldNaoContarAtrasadaWhenPrazoAindaNaoVenceuAoFimDoDia() {
    // Arrange
    persistirComPrioridade(
        StatusSolicitacao.A_FAZER, "2026-09-07T13:00:00Z", null, PrioridadeSolicitacao.BAIXA);

    // Act
    final List<Object[]> linhas = seriePainel("day", null);

    // Assert
    assertEquals(List.of(1L, 1L, 1L, 1L, 1L, 1L, 1L), coluna(linhas, 4));
    assertEquals(List.of(0L, 0L, 0L, 0L, 0L, 0L, 0L), coluna(linhas, 5));
  }

  @Test
  void shouldNuncaContarAtrasadaWhenSolicitacaoNaoTemPrioridade() {
    // Arrange
    persistirAberta("2026-09-07T13:00:00Z");

    // Act
    final List<Object[]> linhas = seriePainel("day", null);

    // Assert
    assertEquals(List.of(0L, 0L, 0L, 0L, 0L, 0L, 0L), coluna(linhas, 5));
  }

  @Test
  void shouldUsarOInstanteAgoraNoDiaDeHojeWhenPrazoVenceDepoisDeAgoraEAntesDoFimDoDia() {
    // Arrange
    persistirComPrioridade(
        StatusSolicitacao.A_FAZER, "2026-09-13T14:00:00Z", null, PrioridadeSolicitacao.URGENTE);
    persistirComPrioridade(
        StatusSolicitacao.A_FAZER, "2026-09-13T10:00:00Z", null, PrioridadeSolicitacao.URGENTE);

    // Act
    final List<Object[]> linhas = seriePainel("day", null);

    // Assert
    assertEquals(2L, ((Number) linhas.get(6)[4]).longValue());
    assertEquals(1L, ((Number) linhas.get(6)[5]).longValue());
  }

  @Test
  void shouldFiltrarPeloModeloNaSerieWhenModeloVemPreenchido() {
    // Arrange
    final UUID outroModelo = criarModelo("DISA");
    final Instant criadaEm = Instant.parse("2026-09-09T13:00:00Z");
    persistir(StatusSolicitacao.A_FAZER, criadaEm, null);
    persistir(StatusSolicitacao.A_FAZER, criadaEm, null, usuarioId, outroModelo, null);

    // Act
    final List<Object[]> linhas = seriePainel("day", outroModelo);

    // Assert
    assertEquals(List.of(0L, 0L, 1L, 0L, 0L, 0L, 0L), coluna(linhas, 1));
    assertEquals(List.of(0L, 0L, 1L, 1L, 1L, 1L, 1L), coluna(linhas, 4));
  }

  @Test
  void shouldAgruparPorSemanaComEstadoNoFimDaSemanaWhenUnidadeESemana() {
    // Arrange
    final Instant criadaEm = Instant.parse("2026-09-10T13:00:00Z");
    persistir(StatusSolicitacao.CONCLUIDA, criadaEm, Instant.parse("2026-09-15T13:00:00Z"));
    persistir(StatusSolicitacao.A_FAZER, Instant.parse("2026-09-08T13:00:00Z"), null);

    // Act
    final List<Object[]> linhas =
        repository.painelSerie(
            LocalDate.of(2026, 9, 9),
            LocalDate.of(2026, 9, 17),
            null,
            "week",
            FUSO,
            Instant.parse("2026-09-16T15:00:00Z"));

    // Assert
    assertEquals(2, linhas.size());
    assertEquals("2026-09-07", linhas.get(0)[0].toString());
    assertEquals("2026-09-14", linhas.get(1)[0].toString());
    assertEquals(List.of(1L, 0L), coluna(linhas, 1));
    assertEquals(List.of(0L, 1L), coluna(linhas, 2));
    assertEquals(List.of(2L, 1L), coluna(linhas, 4));
  }

  @Test
  void shouldContarSoAsEmAbertoWhenSituacaoAgora() {
    // Arrange
    persistirAberta("2026-09-10T13:00:00Z");
    persistir(StatusSolicitacao.EM_ANDAMENTO, Instant.parse("2026-09-10T13:00:00Z"), null);
    persistir(StatusSolicitacao.EM_VALIDACAO, Instant.parse("2026-09-10T13:00:00Z"), null);
    persistir(
        StatusSolicitacao.CONCLUIDA,
        Instant.parse("2026-09-10T13:00:00Z"),
        Instant.parse("2026-09-11T13:00:00Z"));
    persistir(
        StatusSolicitacao.CANCELADA,
        Instant.parse("2026-09-10T13:00:00Z"),
        Instant.parse("2026-09-11T13:00:00Z"));

    // Act
    final Object[] agoraRow =
        repository.painelAgora(null, PAINEL_AGORA_DOMINGO_13_09_12H_BRASILIA).get(0);

    // Assert
    assertEquals(3L, ((Number) agoraRow[0]).longValue());
  }

  @Test
  void shouldContarSoAbertaVencidaWhenAtrasadasAgora() {
    // Arrange
    persistirComPrioridade(
        StatusSolicitacao.A_FAZER, "2026-09-12T13:00:00Z", null, PrioridadeSolicitacao.URGENTE);
    persistirComPrioridade(
        StatusSolicitacao.A_FAZER, "2026-09-13T14:00:00Z", null, PrioridadeSolicitacao.URGENTE);
    persistirComPrioridade(StatusSolicitacao.A_FAZER, "2026-09-01T13:00:00Z", null, null);
    persistirComPrioridade(
        StatusSolicitacao.CONCLUIDA,
        "2026-09-01T13:00:00Z",
        "2026-09-12T13:00:00Z",
        PrioridadeSolicitacao.URGENTE);

    // Act
    final Object[] agoraRow =
        repository.painelAgora(null, PAINEL_AGORA_DOMINGO_13_09_12H_BRASILIA).get(0);

    // Assert
    assertEquals(3L, ((Number) agoraRow[0]).longValue());
    assertEquals(1L, ((Number) agoraRow[1]).longValue());
  }

  @Test
  void shouldFiltrarPeloModeloWhenSituacaoAgora() {
    // Arrange
    final UUID outroModelo = criarModelo("DISA");
    persistirAberta("2026-09-10T13:00:00Z");
    persistir(
        StatusSolicitacao.A_FAZER,
        Instant.parse("2026-09-10T13:00:00Z"),
        null,
        usuarioId,
        outroModelo,
        PrioridadeSolicitacao.URGENTE);

    // Act
    final Object[] agoraRow =
        repository.painelAgora(outroModelo, PAINEL_AGORA_DOMINGO_13_09_12H_BRASILIA).get(0);

    // Assert
    assertEquals(1L, ((Number) agoraRow[0]).longValue());
    assertEquals(1L, ((Number) agoraRow[1]).longValue());
  }

  private void persistirDoTipo(
      final TipoSolicitacao tipo, final Instant criadaEm, final Duration resolucao) {
    final SolicitacaoJpaEntity s =
        persistir(
            StatusSolicitacao.CONCLUIDA,
            criadaEm,
            criadaEm.plus(resolucao),
            usuarioId,
            modeloId,
            null);
    s.setTipo(tipo);
    repository.saveAndFlush(s);
  }

  private Object[] linhaDoRanking() {
    return repository
        .findMetricasPorModelo(
            PageRequest.of(0, 50, Sort.by(Sort.Direction.ASC, "tempo_medio_resolucao")))
        .getContent()
        .stream()
        .filter(linha -> modeloId.equals(linha[0]))
        .findFirst()
        .orElse(null);
  }

  private void persistirUmaCriacaoEDuasManutencoes() {
    final Instant base = agora.minus(Duration.ofDays(40));
    persistirDoTipo(TipoSolicitacao.REPARO, base.plus(Duration.ofDays(10)), Duration.ofDays(1));
    persistirDoTipo(TipoSolicitacao.CRIACAO, base.plus(Duration.ofDays(20)), Duration.ofDays(5));
    persistirDoTipo(TipoSolicitacao.INSPECAO, base.plus(Duration.ofDays(30)), Duration.ofDays(3));
  }

  @Test
  void shouldFicarForaDoRankingWhenModeloTemSoCriacaoConcluida() {
    // Arrange
    persistirDoTipo(TipoSolicitacao.CRIACAO, agora.minus(Duration.ofDays(10)), Duration.ofDays(2));

    // Act
    final Object[] linha = linhaDoRanking();

    // Assert
    assertNull(linha);
  }

  @Test
  void shouldContarSoModelosComManutencaoConcluidaWhenRankingTemModeloSoComCriacao() {
    // Arrange
    final Instant criadaEm = agora.minus(Duration.ofDays(10));
    persistirDoTipo(TipoSolicitacao.CRIACAO, criadaEm, Duration.ofDays(2));
    persistir(
        StatusSolicitacao.CONCLUIDA,
        criadaEm,
        criadaEm.plus(Duration.ofDays(1)),
        usuarioId,
        criarModelo("DISA"),
        null);

    // Act
    final var pagina =
        repository.findMetricasPorModelo(PageRequest.of(0, 1, Sort.by("tempo_medio_resolucao")));

    // Assert
    assertEquals(1, pagina.getTotalElements());
  }

  @Test
  void shouldAparecerSemOTempoDaCriacaoWhenModeloTemUmaManutencaoConcluida() {
    // Arrange
    final Instant base = agora.minus(Duration.ofDays(30));
    persistirDoTipo(TipoSolicitacao.CRIACAO, base, Duration.ofDays(9));
    persistirDoTipo(TipoSolicitacao.REPARO, base.plus(Duration.ofDays(12)), Duration.ofDays(2));

    // Act
    final Object[] linha = linhaDoRanking();

    // Assert
    assertEquals(Duration.ofDays(2).getSeconds(), ((Number) linha[2]).doubleValue(), 0.001);
  }

  @Test
  void shouldIgnorarACriacaoNoIntervaloWhenRankeiaModeloComManutencoes() {
    // Arrange
    persistirUmaCriacaoEDuasManutencoes();

    // Act
    final Object[] linha = linhaDoRanking();

    // Assert
    assertEquals(Duration.ofDays(20).getSeconds(), ((Number) linha[3]).doubleValue(), 0.001);
  }

  @Test
  void shouldExcluirACriacaoDoTempoMedioEDoIntervaloWhenResumePorModelo() {
    // Arrange
    persistirUmaCriacaoEDuasManutencoes();

    // Act
    final Object[] row = repository.resumirPorModelo(modeloId).get(0);
    final double intervalo =
        Duration.between((Instant) row[4], (Instant) row[5]).toMillis()
            / 1000.0
            / (((Number) row[7]).longValue() - 1);

    // Assert
    assertEquals(Duration.ofDays(2).getSeconds(), ((Number) row[6]).doubleValue(), 0.001);
    assertEquals(Duration.ofDays(20).getSeconds(), intervalo, 0.001);
  }

  @Test
  void shouldContarACriacaoNoTotalWhenResumePorModelo() {
    // Arrange
    persistirUmaCriacaoEDuasManutencoes();

    // Act
    final Object[] row = repository.resumirPorModelo(modeloId).get(0);

    // Assert
    assertEquals(3L, ((Number) row[0]).longValue());
    assertEquals(2L, ((Number) row[7]).longValue());
  }

  @Test
  void shouldDevolverTemposVaziosWhenResumeModeloComSoCriacao() {
    // Arrange
    persistirDoTipo(TipoSolicitacao.CRIACAO, agora.minus(Duration.ofDays(10)), Duration.ofDays(2));

    // Act
    final Object[] row = repository.resumirPorModelo(modeloId).get(0);

    // Assert
    assertEquals(1L, ((Number) row[0]).longValue());
    assertNull(row[4]);
    assertNull(row[6]);
    assertEquals(0L, ((Number) row[7]).longValue());
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

  private Page<SolicitacaoJpaEntity> buscar(final String busca, final UUID visivelPara) {
    return repository.findByFilters(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        false,
        visivelPara,
        busca,
        CRITERIO_PADRAO,
        true,
        PRIMEIRA_PAGINA);
  }

  private UUID persistirComTexto(
      final String titulo, final String descricao, final UUID modelo, final UUID abertaPor) {
    final SolicitacaoJpaEntity s =
        persistir(StatusSolicitacao.A_FAZER, agora, null, abertaPor, modelo, null);
    jdbc.update(
        "UPDATE solicitacoes SET titulo = ?, descricao = ? WHERE id = ?",
        titulo,
        descricao,
        s.getId());
    return s.getId();
  }

  @Test
  void shouldCasarTituloDescricaoECodigoDoModeloSemCaixaESemAcentoWhenBusca() {
    // Arrange
    final UUID modeloCorreia = criarModelo("FBOX");
    jdbc.update("UPDATE modelos SET codigo = 'CORREIA-77' WHERE id = ?", modeloCorreia);
    final UUID noTitulo = persistirComTexto("Trocar CORREIA", "x", modeloId, usuarioId);
    final UUID naDescricao =
        persistirComTexto("Reparo", "A correia está gasta", modeloId, usuarioId);
    final UUID noCodigo = persistirComTexto("Revisão", "y", modeloCorreia, usuarioId);
    final UUID fora = persistirComTexto("Pintura", "tinta", modeloId, usuarioId);

    // Act
    final var ids = ids(buscar("correía", null));

    // Assert
    assertEquals(List.of(noTitulo, naDescricao, noCodigo).stream().sorted().toList(), ids);
    assertFalse(ids.contains(fora));
  }

  @Test
  void shouldTratarPercentualComoTextoWhenBuscaEscapada() {
    // Arrange
    final UUID literal = persistirComTexto("Reduzir 50% da carga", "x", modeloId, usuarioId);
    persistirComTexto("Reduzir 500 da carga", "x", modeloId, usuarioId);

    // Act
    final var ids = ids(buscar("50\\%", null));

    // Assert
    assertEquals(List.of(literal), ids);
  }

  @Test
  void shouldRespeitarAVisibilidadeDoOperadorWhenBusca() {
    // Arrange
    final UUID outro = criarUsuario();
    final UUID minha = persistirComTexto("Trocar correia", "x", modeloId, usuarioId);
    persistirComTexto("Trocar correia do outro", "x", modeloId, outro);

    // Act
    final var ids = ids(buscar("correia", usuarioId));

    // Assert
    assertEquals(List.of(minha), ids);
  }

  @Test
  void shouldTrazerAEncerradaMaisRecentementePrimeiroWhenOrdenaPorEncerramentoDecrescente() {
    // Arrange
    final UUID concluidaAntiga =
        persistir(
                StatusSolicitacao.CONCLUIDA,
                agora.minus(Duration.ofDays(9)),
                agora.minus(Duration.ofDays(5)))
            .getId();
    final UUID canceladaRecente =
        persistir(
                StatusSolicitacao.CANCELADA,
                agora.minus(Duration.ofDays(8)),
                agora.minus(Duration.ofDays(1)))
            .getId();
    final UUID concluidaMeio =
        persistir(
                StatusSolicitacao.CONCLUIDA,
                agora.minus(Duration.ofDays(7)),
                agora.minus(Duration.ofDays(3)))
            .getId();
    final UUID emAberto =
        persistir(StatusSolicitacao.A_FAZER, agora.minus(Duration.ofDays(2)), null).getId();

    // Act
    final var page =
        listarOrdenado(CriterioOrdenacaoSolicitacao.ENCERRADA_EM, false, PRIMEIRA_PAGINA);

    // Assert
    assertEquals(
        List.of(canceladaRecente, concluidaMeio, concluidaAntiga, emAberto), idsNaOrdem(page));
  }

  @Test
  void shouldContarSoAsEmAbertoPorTipoEPorPrioridadeWhenAgrega() {
    // Arrange
    persistir(StatusSolicitacao.A_FAZER, agora, null);
    persistir(
        StatusSolicitacao.EM_ANDAMENTO,
        agora,
        null,
        usuarioId,
        modeloId,
        PrioridadeSolicitacao.ALTA);
    persistir(
        StatusSolicitacao.EM_ANDAMENTO,
        agora,
        null,
        usuarioId,
        modeloId,
        PrioridadeSolicitacao.ALTA);
    persistir(
        StatusSolicitacao.CONCLUIDA,
        agora,
        agora,
        usuarioId,
        modeloId,
        PrioridadeSolicitacao.BAIXA);

    // Act
    final var porTipo = repository.countEmAbertoPorTipo();
    final var porPrioridade = repository.countEmAbertoPorPrioridade();

    // Assert
    assertEquals(1, porTipo.size());
    assertEquals(3L, porTipo.get(0)[1]);
    assertEquals(
        2L,
        porPrioridade.stream()
            .filter(l -> l[0] == PrioridadeSolicitacao.ALTA)
            .findFirst()
            .get()[1]);
    assertEquals(1L, porPrioridade.stream().filter(l -> l[0] == null).findFirst().get()[1]);
    assertEquals(2, porPrioridade.size());
  }
}
