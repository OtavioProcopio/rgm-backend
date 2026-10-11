package com.rgm.api.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.rgm.api.adapter.out.persistence.entity.SolicitacaoJpaEntity;
import com.rgm.api.adapter.out.persistence.repository.SolicitacaoJpaRepository;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.enums.CriterioOrdenacaoSolicitacao;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.OrdenacaoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class SolicitacaoRepositoryAdapterTest {

  private SolicitacaoJpaRepository jpa;
  private SolicitacaoRepositoryAdapter adapter;

  @BeforeEach
  void setUp() {
    jpa = mock(SolicitacaoJpaRepository.class);
    adapter = new SolicitacaoRepositoryAdapter(jpa);
  }

  private SolicitacaoJpaEntity criarEntity() {
    final SolicitacaoJpaEntity e = new SolicitacaoJpaEntity();
    e.setId(UUID.randomUUID());
    e.setTitulo("Trocar peça");
    e.setDescricao("Desc");
    e.setTipo(TipoSolicitacao.REPARO);
    e.setStatus(StatusSolicitacao.A_FAZER);
    e.setPrioridade(PrioridadeSolicitacao.MEDIA);
    e.setModeloId(UUID.randomUUID());
    e.setAbertaPorUsuarioId(UUID.randomUUID());
    e.setCriadaEm(Instant.now());
    e.setAtualizadaEm(Instant.now());
    return e;
  }

  private Solicitacao criarDomain() {
    return new Solicitacao(
        UUID.randomUUID(),
        "Trocar peça",
        "Desc",
        TipoSolicitacao.REPARO,
        StatusSolicitacao.A_FAZER,
        PrioridadeSolicitacao.MEDIA,
        UUID.randomUUID(),
        null /* modeloCodigo */,
        null /* modeloMaquina */,
        null /* modeloObservacoes */,
        UUID.randomUUID(),
        null,
        Instant.now(),
        Instant.now(),
        null,
        null);
  }

  @Test
  void findById_quandoExistir_retornaSolicitacao() {
    final UUID id = UUID.randomUUID();
    final SolicitacaoJpaEntity e = criarEntity();
    e.setId(id);
    when(jpa.findById(id)).thenReturn(Optional.of(e));

    final Optional<Solicitacao> result = adapter.findById(id);

    assertTrue(result.isPresent());
    assertEquals("Trocar peça", result.get().getTitulo());
  }

  @Test
  void findById_quandoNaoExistir_retornaVazio() {
    final UUID id = UUID.randomUUID();
    when(jpa.findById(id)).thenReturn(Optional.empty());

    assertFalse(adapter.findById(id).isPresent());
  }

  @Test
  void save_persisteERetorna() {
    final Solicitacao domain = criarDomain();
    final SolicitacaoJpaEntity e = criarEntity();
    when(jpa.save(any(SolicitacaoJpaEntity.class))).thenReturn(e);

    final Solicitacao result = adapter.save(domain);

    assertNotNull(result);
    verify(jpa).save(any(SolicitacaoJpaEntity.class));
  }

  @Test
  void deleteById_delegaAoJpa() {
    final UUID id = UUID.randomUUID();
    adapter.deleteById(id);
    verify(jpa).deleteById(id);
  }

  @Test
  void existsByModeloIdAndStatusIn_delegaAoJpa() {
    final UUID modeloId = UUID.randomUUID();
    when(jpa.existsByModeloIdAndStatusIn(modeloId, List.of(StatusSolicitacao.A_FAZER)))
        .thenReturn(true);

    assertTrue(adapter.existsByModeloIdAndStatusIn(modeloId, List.of(StatusSolicitacao.A_FAZER)));
  }

  @Test
  void existsByModeloId_delegaAoJpa() {
    final UUID id = UUID.randomUUID();
    when(jpa.existsByModeloId(id)).thenReturn(false);

    assertFalse(adapter.existsByModeloId(id));
  }

  @Test
  void existsByAbertaPorUsuarioId_delegaAoJpa() {
    final UUID id = UUID.randomUUID();
    when(jpa.existsByAbertaPorUsuarioId(id)).thenReturn(true);

    assertTrue(adapter.existsByAbertaPorUsuarioId(id));
  }

  @Test
  void findByModeloId_retornaLista() {
    final UUID modeloId = UUID.randomUUID();
    final SolicitacaoJpaEntity e = criarEntity();
    e.setModeloId(modeloId);
    when(jpa.findByModeloId(modeloId)).thenReturn(List.of(e));

    final List<Solicitacao> result = adapter.findByModeloId(modeloId);

    assertEquals(1, result.size());
  }

  @Test
  void findAll_retornaPaginado() {
    final SolicitacaoJpaEntity e = criarEntity();
    final Page<SolicitacaoJpaEntity> page = new PageImpl<>(List.of(e), PageRequest.of(0, 10), 1);
    when(jpa.findAll(any(PageRequest.class))).thenReturn(page);

    final PageResult<Solicitacao> result = adapter.findAll(0, 10);

    assertEquals(1, result.content().size());
    assertEquals(1L, result.totalElements());
  }

  @Test
  void shouldConverterEnumsEmTextoEMapearAPaginaWhenFiltraPorStatusETipo() {
    // Arrange
    final SolicitacaoJpaEntity e = criarEntity();
    final var pageable = PageRequest.of(0, 10);
    final Page<SolicitacaoJpaEntity> page = new PageImpl<>(List.of(e), pageable, 1);
    when(jpa.findByFilters(
            StatusSolicitacao.A_FAZER.name(),
            null,
            TipoSolicitacao.REPARO.name(),
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
            "CRIADA_EM",
            true,
            pageable))
        .thenReturn(page);

    // Act
    final PageResult<Solicitacao> result =
        adapter.findByFilters(
            StatusSolicitacao.A_FAZER,
            null,
            TipoSolicitacao.REPARO,
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
            OrdenacaoSolicitacao.PADRAO,
            0,
            10);

    // Assert
    assertEquals(List.of(e.getId()), result.content().stream().map(Solicitacao::getId).toList());
    verify(jpa, times(1))
        .findByFilters(
            StatusSolicitacao.A_FAZER.name(),
            null,
            TipoSolicitacao.REPARO.name(),
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
            "CRIADA_EM",
            true,
            pageable);
    verifyNoMoreInteractions(jpa);
  }

  @Test
  void shouldRepassarAVisibilidadeAoRepositorioWhenFiltraParaUmOperador() {
    // Arrange
    final UUID operadorId = UUID.randomUUID();
    final var pageable = PageRequest.of(0, 10);
    final Page<SolicitacaoJpaEntity> page = new PageImpl<>(List.of(), pageable, 0);
    when(jpa.findByFilters(
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
            operadorId,
            null,
            "CRIADA_EM",
            true,
            pageable))
        .thenReturn(page);

    // Act
    final PageResult<Solicitacao> result =
        adapter.findByFilters(
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
            operadorId,
            null,
            OrdenacaoSolicitacao.PADRAO,
            0,
            10);

    // Assert
    assertEquals(new PageResult<>(List.of(), 0, 10, 0, 0), result);
    verify(jpa, times(1))
        .findByFilters(
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
            operadorId,
            null,
            "CRIADA_EM",
            true,
            pageable);
    verifyNoMoreInteractions(jpa);
  }

  @Test
  void countGroupByModeloId_retornaMapa() {
    final UUID modeloId = UUID.randomUUID();
    final Object[] row = {modeloId, 3L};
    final List<Object[]> rows = new ArrayList<>();
    rows.add(row);
    when(jpa.countGroupByModeloId()).thenReturn(rows);

    final Map<UUID, Long> result = adapter.countGroupByModeloId();

    assertEquals(1, result.size());
    assertEquals(3L, result.get(modeloId));
  }

  @Test
  void countGroupByModeloId_ignoraLinhaComModeloIdNulo() {
    // Solicitacoes CRIACAO ainda nao concluidas nao tem modelo_id — a linha nula
    // do GROUP BY precisa ser descartada, senao a resposta JSON quebra (Map com
    // chave nula nao e serializavel).
    final UUID modeloId = UUID.randomUUID();
    final List<Object[]> rows = new ArrayList<>();
    rows.add(new Object[] {modeloId, 2L});
    rows.add(new Object[] {null, 5L});
    when(jpa.countGroupByModeloId()).thenReturn(rows);

    final Map<UUID, Long> result = adapter.countGroupByModeloId();

    assertEquals(1, result.size());
    assertEquals(2L, result.get(modeloId));
    assertFalse(result.containsKey(null));
  }

  @Test
  void count_delegaAoJpa() {
    when(jpa.count()).thenReturn(42L);

    assertEquals(42L, adapter.count());
  }

  @Test
  void countByStatus_delegaAoJpa() {
    when(jpa.countByStatus(StatusSolicitacao.A_FAZER)).thenReturn(5L);

    assertEquals(5L, adapter.countByStatus(StatusSolicitacao.A_FAZER));
  }

  @Test
  void shouldMapearALinhaAgregadaWhenAgregaSeriePorDia() {
    // Arrange
    final Instant inicio = Instant.parse("2026-09-01T03:00:00Z");
    final Instant fim = Instant.parse("2026-10-01T03:00:00Z");
    final UUID modeloId = UUID.randomUUID();
    final java.time.ZoneId fuso = java.time.ZoneId.of("America/Sao_Paulo");
    final List<Object[]> linhas = new ArrayList<>();
    linhas.add(new Object[] {java.sql.Date.valueOf("2026-09-09"), 5L, 2L, 2L, 1L, 7200.5});
    linhas.add(new Object[] {java.sql.Date.valueOf("2026-09-10"), 1L, 1L, 0L, 0L, null});
    when(jpa.agregarSerie(inicio, fim, modeloId, "day", "America/Sao_Paulo")).thenReturn(linhas);

    // Act
    final var serie =
        adapter.agregarSerie(
            inicio,
            fim,
            modeloId,
            com.rgm.api.core.domain.ports.repositories.GranularidadeSerie.DIA,
            fuso);

    // Assert
    assertEquals(
        List.of(
            new com.rgm.api.core.domain.ports.repositories.LinhaDeSerie(
                java.time.LocalDate.of(2026, 9, 9), 5, 2, 2, 1, 7200.5),
            new com.rgm.api.core.domain.ports.repositories.LinhaDeSerie(
                java.time.LocalDate.of(2026, 9, 10), 1, 1, 0, 0, null)),
        serie);
    verify(jpa, times(1)).agregarSerie(inicio, fim, modeloId, "day", "America/Sao_Paulo");
    verifyNoMoreInteractions(jpa);
  }

  @Test
  void shouldPedirAUnidadeSemanaWhenGranularidadeESemana() {
    // Arrange
    final Instant inicio = Instant.parse("2026-09-01T03:00:00Z");
    final Instant fim = Instant.parse("2026-10-01T03:00:00Z");
    final java.time.ZoneId fuso = java.time.ZoneId.of("America/Sao_Paulo");
    when(jpa.agregarSerie(inicio, fim, null, "week", "America/Sao_Paulo")).thenReturn(List.of());

    // Act
    final var serie =
        adapter.agregarSerie(
            inicio,
            fim,
            null,
            com.rgm.api.core.domain.ports.repositories.GranularidadeSerie.SEMANA,
            fuso);

    // Assert
    assertTrue(serie.isEmpty());
    verify(jpa, times(1)).agregarSerie(inicio, fim, null, "week", "America/Sao_Paulo");
    verifyNoMoreInteractions(jpa);
  }

  private static final java.time.ZoneId FUSO_PAINEL = java.time.ZoneId.of("America/Sao_Paulo");
  private static final Instant PAINEL_INICIO = Instant.parse("2026-09-07T03:00:00Z");
  private static final Instant PAINEL_FIM = Instant.parse("2026-09-14T03:00:00Z");
  private static final Instant PAINEL_AGORA = Instant.parse("2026-09-13T15:00:00Z");

  @Test
  void shouldMapearTotaisESerieDoPainelWhenAgregaAJanela() {
    // Arrange
    final UUID modeloId = UUID.randomUUID();
    final List<Object[]> totais = new ArrayList<>();
    totais.add(new Object[] {4L, 3L, 1L, 1800.0});
    final List<Object[]> serie = new ArrayList<>();
    serie.add(new Object[] {java.sql.Date.valueOf("2026-09-07"), 2L, 1L, 0L, 5L, 2L});
    serie.add(new Object[] {java.sql.Date.valueOf("2026-09-08"), 0L, 0L, 1L, 4L, 1L});
    when(jpa.painelTotais(PAINEL_INICIO, PAINEL_FIM, modeloId)).thenReturn(totais);
    when(jpa.painelSerie(
            java.time.LocalDate.of(2026, 9, 7),
            java.time.LocalDate.of(2026, 9, 14),
            modeloId,
            "day",
            "America/Sao_Paulo",
            PAINEL_AGORA))
        .thenReturn(serie);

    // Act
    final var painel =
        adapter.agregarPainel(
            PAINEL_INICIO,
            PAINEL_FIM,
            modeloId,
            com.rgm.api.core.domain.ports.repositories.GranularidadeSerie.DIA,
            FUSO_PAINEL,
            PAINEL_AGORA);

    // Assert
    assertEquals(
        new com.rgm.api.core.domain.ports.repositories.PainelAgregado.Totais(4, 3, 1, 1800.0),
        painel.totais());
    assertEquals(
        List.of(
            new com.rgm.api.core.domain.ports.repositories.PainelAgregado.Ponto(
                java.time.LocalDate.of(2026, 9, 7), 2, 1, 0, 5, 2),
            new com.rgm.api.core.domain.ports.repositories.PainelAgregado.Ponto(
                java.time.LocalDate.of(2026, 9, 8), 0, 0, 1, 4, 1)),
        painel.serie());
    verify(jpa, times(1)).painelTotais(PAINEL_INICIO, PAINEL_FIM, modeloId);
    verify(jpa, times(1))
        .painelSerie(
            java.time.LocalDate.of(2026, 9, 7),
            java.time.LocalDate.of(2026, 9, 14),
            modeloId,
            "day",
            "America/Sao_Paulo",
            PAINEL_AGORA);
    verifyNoMoreInteractions(jpa);
  }

  @Test
  void shouldPedirAUnidadeSemanaEManterTempoNuloWhenPainelEmSemanas() {
    // Arrange
    final List<Object[]> totais = new ArrayList<>();
    totais.add(new Object[] {0L, 0L, 0L, null});
    when(jpa.painelTotais(PAINEL_INICIO, PAINEL_FIM, null)).thenReturn(totais);
    when(jpa.painelSerie(
            java.time.LocalDate.of(2026, 9, 7),
            java.time.LocalDate.of(2026, 9, 14),
            null,
            "week",
            "America/Sao_Paulo",
            PAINEL_AGORA))
        .thenReturn(List.of());

    // Act
    final var painel =
        adapter.agregarPainel(
            PAINEL_INICIO,
            PAINEL_FIM,
            null,
            com.rgm.api.core.domain.ports.repositories.GranularidadeSerie.SEMANA,
            FUSO_PAINEL,
            PAINEL_AGORA);

    // Assert
    assertNull(painel.totais().tempoMedioResolucaoSegundos());
    assertTrue(painel.serie().isEmpty());
    verify(jpa, times(1)).painelTotais(PAINEL_INICIO, PAINEL_FIM, null);
    verify(jpa, times(1))
        .painelSerie(
            java.time.LocalDate.of(2026, 9, 7),
            java.time.LocalDate.of(2026, 9, 14),
            null,
            "week",
            "America/Sao_Paulo",
            PAINEL_AGORA);
    verifyNoMoreInteractions(jpa);
  }

  @Test
  void shouldMapearAbertasEAtrasadasWhenSituacaoAgora() {
    // Arrange
    final UUID modeloId = UUID.randomUUID();
    final List<Object[]> linhas = new ArrayList<>();
    linhas.add(new Object[] {7L, 2L});
    when(jpa.painelAgora(modeloId, PAINEL_AGORA)).thenReturn(linhas);

    // Act
    final var agora = adapter.situacaoAgora(modeloId, PAINEL_AGORA);

    // Assert
    assertEquals(new com.rgm.api.core.domain.ports.repositories.PainelAgregado.Agora(7, 2), agora);
    verify(jpa, times(1)).painelAgora(modeloId, PAINEL_AGORA);
    verifyNoMoreInteractions(jpa);
  }

  @Test
  void findMetricasPorModelo_mapeiaLinhasEOrdenaPorTempoResolucaoDescendente() {
    final UUID modeloId = UUID.randomUUID();
    final Object[] row = {modeloId, "MDL-001", 3600.0, 7200.0};
    final Page<Object[]> page = new PageImpl<>(List.<Object[]>of(row), PageRequest.of(0, 20), 1);
    final org.mockito.ArgumentCaptor<org.springframework.data.domain.Pageable> captor =
        org.mockito.ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
    when(jpa.findMetricasPorModelo(captor.capture())).thenReturn(page);

    final PageResult<com.rgm.api.core.domain.ports.repositories.MetricaModeloRow> result =
        adapter.findMetricasPorModelo(
            com.rgm.api.core.domain.model.enums.OrdenacaoMetricaModelo.TEMPO_RESOLUCAO,
            false,
            0,
            20);

    assertEquals(1, result.content().size());
    assertEquals(modeloId, result.content().get(0).modeloId());
    assertEquals("MDL-001", result.content().get(0).codigo());
    assertEquals(3600.0, result.content().get(0).tempoMedioResolucaoSegundos());
    assertEquals(7200.0, result.content().get(0).intervaloMedioSegundos());

    final var sort = captor.getValue().getSort().getOrderFor("tempo_medio_resolucao");
    assertNotNull(sort);
    assertTrue(sort.isDescending());
  }

  @Test
  void findMetricasPorModelo_ordenaPorIntervaloAscendenteEAceitaIntervaloNulo() {
    final UUID modeloId = UUID.randomUUID();
    final Object[] row = {modeloId, "MDL-002", 1800.0, null};
    final Page<Object[]> page = new PageImpl<>(List.<Object[]>of(row), PageRequest.of(1, 10), 1);
    final org.mockito.ArgumentCaptor<org.springframework.data.domain.Pageable> captor =
        org.mockito.ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
    when(jpa.findMetricasPorModelo(captor.capture())).thenReturn(page);

    final PageResult<com.rgm.api.core.domain.ports.repositories.MetricaModeloRow> result =
        adapter.findMetricasPorModelo(
            com.rgm.api.core.domain.model.enums.OrdenacaoMetricaModelo.INTERVALO, true, 1, 10);

    assertNull(result.content().get(0).intervaloMedioSegundos());
    final var sort = captor.getValue().getSort().getOrderFor("intervalo_medio");
    assertNotNull(sort);
    assertTrue(sort.isAscending());
  }

  @Test
  void shouldRepassarEmAbertoAoJpaWhenFiltroVemLigado() {
    // Arrange
    final var pageable = PageRequest.of(0, 10);
    final Page<SolicitacaoJpaEntity> page = new PageImpl<>(List.of(), pageable, 0);
    when(jpa.findByFilters(
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
            true,
            null,
            null,
            "CRIADA_EM",
            true,
            pageable))
        .thenReturn(page);

    // Act
    final PageResult<Solicitacao> result =
        adapter.findByFilters(
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
            true,
            null,
            null,
            OrdenacaoSolicitacao.PADRAO,
            0,
            10);

    // Assert
    assertEquals(new PageResult<>(List.of(), 0, 10, 0, 0), result);
    verify(jpa, times(1))
        .findByFilters(
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
            true,
            null,
            null,
            "CRIADA_EM",
            true,
            pageable);
    verifyNoMoreInteractions(jpa);
  }

  @Test
  void shouldRepassarCriterioESentidoAoJpaWhenOrdenacaoEPrazoLimiteDecrescente() {
    // Arrange
    final var pageable = PageRequest.of(2, 10);
    final Page<SolicitacaoJpaEntity> page = new PageImpl<>(List.of(), pageable, 0);
    final OrdenacaoSolicitacao ordenacao =
        new OrdenacaoSolicitacao(CriterioOrdenacaoSolicitacao.PRAZO_LIMITE, false);
    when(jpa.findByFilters(
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
            null,
            null,
            "PRAZO_LIMITE",
            false,
            pageable))
        .thenReturn(page);

    // Act
    final PageResult<Solicitacao> result =
        adapter.findByFilters(
            null, null, null, null, null, null, null, null, null, null, null, null, false, null,
            null, ordenacao, 2, 10);

    // Assert
    assertEquals(new PageResult<>(List.of(), 2, 10, 0, 0), result);
    verify(jpa, times(1))
        .findByFilters(
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
            null,
            null,
            "PRAZO_LIMITE",
            false,
            pageable);
    verifyNoMoreInteractions(jpa);
  }

  @Test
  void shouldMandarPaginaSemSortAoJpaWhenOrdenacaoVemDentroDaConsulta() {
    // Arrange
    final var captor =
        org.mockito.ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
    final Page<SolicitacaoJpaEntity> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
    when(jpa.findByFilters(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            org.mockito.ArgumentMatchers.anyBoolean(),
            any(),
            any(),
            any(),
            org.mockito.ArgumentMatchers.anyBoolean(),
            captor.capture()))
        .thenReturn(page);

    // Act
    adapter.findByFilters(
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
        null,
        null,
        OrdenacaoSolicitacao.PADRAO,
        0,
        10);

    // Assert
    assertTrue(captor.getValue().getSort().isUnsorted());
  }

  @Test
  void shouldMontarOResumoComOIntervaloCalculadoWhenResumePorModelo() {
    // Arrange
    final UUID modeloId = UUID.randomUUID();
    final Instant primeira = Instant.parse("2026-09-01T12:00:00Z");
    final Instant ultima = primeira.plusSeconds(300);
    final List<Object[]> linhas = new ArrayList<>();
    linhas.add(new Object[] {5L, 1L, 2L, 1L, primeira, ultima, 259200.0, 4L});
    when(jpa.resumirPorModelo(modeloId)).thenReturn(linhas);
    final var esperado =
        new com.rgm.api.core.domain.ports.repositories.ResumoSolicitacoesModelo(
            5, 1, 2, 1, 259200.0, 100.0);

    // Act
    final var resumo = adapter.resumirPorModelo(modeloId);

    // Assert
    assertEquals(esperado, resumo);
    verify(jpa, org.mockito.Mockito.times(1)).resumirPorModelo(modeloId);
    verifyNoMoreInteractions(jpa);
  }

  @Test
  void shouldOrdenarPorCriadaEmEDesempatarPorIdWhenListaTodasPaginadas() {
    // Arrange
    final var captor =
        org.mockito.ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
    final Page<SolicitacaoJpaEntity> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
    when(jpa.findAll(captor.capture())).thenReturn(page);
    final var esperado =
        org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.ASC, "criadaEm")
            .and(
                org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Direction.ASC, "id"));

    // Act
    adapter.findAll(0, 10);

    // Assert
    assertEquals(esperado, captor.getValue().getSort());
    verify(jpa, times(1)).findAll(captor.getValue());
    verifyNoMoreInteractions(jpa);
  }

  @Test
  void shouldDeixarOsTemposVaziosWhenModeloNaoTemSolicitacao() {
    // Arrange
    final UUID modeloId = UUID.randomUUID();
    final List<Object[]> linhas = new ArrayList<>();
    linhas.add(new Object[] {0L, 0L, 0L, 0L, null, null, null, 0L});
    when(jpa.resumirPorModelo(modeloId)).thenReturn(linhas);
    final var esperado =
        new com.rgm.api.core.domain.ports.repositories.ResumoSolicitacoesModelo(
            0, 0, 0, 0, null, null);

    // Act
    final var resumo = adapter.resumirPorModelo(modeloId);

    // Assert
    assertEquals(esperado, resumo);
  }

  @Test
  void shouldMapEnumNamesAndNullPriorityWhenCountingOpenByTypeAndPriority() {
    // Arrange
    when(jpa.countEmAbertoPorTipo())
        .thenReturn(List.<Object[]>of(new Object[] {TipoSolicitacao.REPARO, 3L}));
    when(jpa.countEmAbertoPorPrioridade())
        .thenReturn(
            List.<Object[]>of(
                new Object[] {PrioridadeSolicitacao.ALTA, 2L}, new Object[] {null, 1L}));

    // Act
    final var porTipo = adapter.countEmAbertoPorTipo();
    final var porPrioridade = adapter.countEmAbertoPorPrioridade();

    // Assert
    assertEquals(java.util.Map.of("REPARO", 3L), porTipo);
    assertEquals(java.util.Map.of("ALTA", 2L, "SEM_PRIORIDADE", 1L), porPrioridade);
  }

  @Test
  void shouldEscapeWildcardsWhenPassingBuscaToJpa() {
    // Arrange
    final var captor = org.mockito.ArgumentCaptor.forClass(String.class);
    final Page<SolicitacaoJpaEntity> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
    when(jpa.findByFilters(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            org.mockito.ArgumentMatchers.anyBoolean(),
            any(),
            captor.capture(),
            any(),
            org.mockito.ArgumentMatchers.anyBoolean(),
            any()))
        .thenReturn(page);

    // Act
    adapter.findByFilters(
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
        null,
        "50%_a\\b",
        OrdenacaoSolicitacao.PADRAO,
        0,
        10);

    // Assert
    assertEquals("50\\%\\_a\\\\b", captor.getValue());
  }
}
