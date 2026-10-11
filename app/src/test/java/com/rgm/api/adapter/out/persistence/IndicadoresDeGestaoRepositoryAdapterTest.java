package com.rgm.api.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rgm.api.adapter.out.persistence.repository.IndicadoresDeGestaoJpaRepository;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IndicadoresDeGestaoRepositoryAdapterTest {

  private static final Instant INICIO = Instant.parse("2026-09-12T03:00:00Z");
  private static final Instant FIM = Instant.parse("2026-10-12T03:00:00Z");

  private IndicadoresDeGestaoJpaRepository jpa;
  private IndicadoresDeGestaoRepositoryAdapter adapter;

  @BeforeEach
  void setUp() {
    jpa = mock(IndicadoresDeGestaoJpaRepository.class);
    adapter = new IndicadoresDeGestaoRepositoryAdapter(jpa);
  }

  @Test
  void shouldMapDeadlineRowsToPriorityAndCounts() {
    // Arrange
    when(jpa.prazoPorPrioridade(INICIO, FIM))
        .thenReturn(List.<Object[]>of(new Object[] {"ALTA", 4L, BigInteger.valueOf(3)}));

    // Act
    final var linhas = adapter.prazoPorPrioridade(INICIO, FIM);

    // Assert
    assertEquals(PrioridadeSolicitacao.ALTA, linhas.get(0).prioridade());
    assertEquals(4, linhas.get(0).concluidas());
    assertEquals(3, linhas.get(0).noPrazo());
  }

  @Test
  void shouldKeepNullPercentilesWhenThereIsNoCompletedSolicitacao() {
    // Arrange
    when(jpa.resolucao(INICIO, FIM)).thenReturn(List.<Object[]>of(new Object[] {0L, null, null}));

    // Act
    final var resolucao = adapter.resolucao(INICIO, FIM);

    // Assert
    assertEquals(0, resolucao.concluidas());
    assertNull(resolucao.medianaSegundos());
    assertNull(resolucao.p90Segundos());
  }

  @Test
  void shouldMapBacklogReturnsAndCountsFromSingleRows() {
    // Arrange
    when(jpa.backlog(FIM)).thenReturn(List.<Object[]>of(new Object[] {5L, 2L, 2L, 1L, 3L}));
    when(jpa.devolucoes(INICIO, FIM)).thenReturn(List.<Object[]>of(new Object[] {4L, 1L}));
    when(jpa.concluidasSemPrioridade(INICIO, FIM)).thenReturn(2L);
    when(jpa.abertasComResponsavel()).thenReturn(7L);

    // Act
    final var backlog = adapter.backlog(FIM);
    final var devolucoes = adapter.devolucoes(INICIO, FIM);

    // Assert
    assertEquals(5, backlog.abertas());
    assertEquals(3, backlog.semResponsavel());
    assertEquals(4, devolucoes.enviadasParaValidacao());
    assertEquals(1, devolucoes.devolucoes());
    assertEquals(2, adapter.concluidasSemPrioridade(INICIO, FIM));
    assertEquals(7, adapter.abertasComResponsavel());
  }

  @Test
  void shouldMapStagesMachinesRecurringModelsAndLoad() {
    // Arrange
    final UUID modelo = UUID.randomUUID();
    final UUID usuario = UUID.randomUUID();
    when(jpa.tempoPorEtapa(INICIO, FIM))
        .thenReturn(List.<Object[]>of(new Object[] {"EM_ANDAMENTO", 2L, 18000.0, 19000.5}));
    when(jpa.porMaquina(INICIO, FIM, FIM))
        .thenReturn(List.<Object[]>of(new Object[] {"FBOX", 5L, 2L, null}));
    when(jpa.reincidentes(INICIO, FIM, 3))
        .thenReturn(List.<Object[]>of(new Object[] {modelo, "MOD-1", 2, "FBOX", 3L}));
    when(jpa.cargaPorResponsavel())
        .thenReturn(List.<Object[]>of(new Object[] {usuario, "Ana", 4L}));

    // Act
    final var etapas = adapter.tempoPorEtapa(INICIO, FIM);
    final var maquinas = adapter.porMaquina(INICIO, FIM, FIM);
    final var reincidentes = adapter.reincidentes(INICIO, FIM, 3);
    final var carga = adapter.cargaPorResponsavel();

    // Assert
    assertEquals("EM_ANDAMENTO", etapas.get(0).etapa());
    assertEquals(19000.5, etapas.get(0).mediaSegundos());
    assertNull(maquinas.get(0).tempoMedioResolucaoSegundos());
    assertEquals(2, maquinas.get(0).emAtraso());
    assertEquals(modelo, reincidentes.get(0).modeloId());
    assertEquals(2, reincidentes.get(0).versao());
    assertEquals("Ana", carga.get(0).nome());
    verify(jpa, times(1)).reincidentes(INICIO, FIM, 3);
  }
}
