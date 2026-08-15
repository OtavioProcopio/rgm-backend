package com.rgm.api.core.application.usecases.solicitacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.model.enums.OrdenacaoMetricaModelo;
import com.rgm.api.core.domain.ports.repositories.MetricaModeloRow;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ObterMetricasPorModeloUseCaseTest {

  private SolicitacaoRepository solicitacaoRepository;
  private ObterMetricasPorModeloUseCase useCase;

  @BeforeEach
  void setUp() {
    solicitacaoRepository = mock(SolicitacaoRepository.class);
    useCase = new ObterMetricasPorModeloUseCase(solicitacaoRepository);
  }

  @Test
  void deveDelegarAoRepositorioComOrdenacaoPorTempoResolucao() {
    final UUID modeloId = UUID.randomUUID();
    final var row = new MetricaModeloRow(modeloId, "MDL-001", 3600.0, 7200.0);
    final var pageResult = new PageResult<>(List.of(row), 0, 20, 1L, 1);

    when(solicitacaoRepository.findMetricasPorModelo(
            OrdenacaoMetricaModelo.TEMPO_RESOLUCAO, false, 0, 20))
        .thenReturn(pageResult);

    final var resultado =
        useCase.execute(
            new ObterMetricasPorModeloUseCase.Input(
                OrdenacaoMetricaModelo.TEMPO_RESOLUCAO, false, 0, 20));

    assertEquals(1, resultado.content().size());
    assertEquals(modeloId, resultado.content().get(0).modeloId());
    assertEquals(3600.0, resultado.content().get(0).tempoMedioResolucaoSegundos());
    assertEquals(7200.0, resultado.content().get(0).intervaloMedioSegundos());
    verify(solicitacaoRepository)
        .findMetricasPorModelo(OrdenacaoMetricaModelo.TEMPO_RESOLUCAO, false, 0, 20);
  }

  @Test
  void deveDelegarAoRepositorioComOrdenacaoPorIntervalo() {
    final var pageResult = new PageResult<MetricaModeloRow>(List.of(), 0, 20, 0L, 0);

    when(solicitacaoRepository.findMetricasPorModelo(OrdenacaoMetricaModelo.INTERVALO, true, 1, 10))
        .thenReturn(pageResult);

    final var resultado =
        useCase.execute(
            new ObterMetricasPorModeloUseCase.Input(OrdenacaoMetricaModelo.INTERVALO, true, 1, 10));

    assertTrue(resultado.content().isEmpty());
  }

  @Test
  void devePermitirIntervaloNuloQuandoModeloTemUmaUnicaSolicitacao() {
    final UUID modeloId = UUID.randomUUID();
    final var row = new MetricaModeloRow(modeloId, "MDL-002", 1800.0, null);
    final var pageResult = new PageResult<>(List.of(row), 0, 20, 1L, 1);

    when(solicitacaoRepository.findMetricasPorModelo(
            OrdenacaoMetricaModelo.TEMPO_RESOLUCAO, true, 0, 20))
        .thenReturn(pageResult);

    final var resultado =
        useCase.execute(
            new ObterMetricasPorModeloUseCase.Input(
                OrdenacaoMetricaModelo.TEMPO_RESOLUCAO, true, 0, 20));

    assertNull(resultado.content().get(0).intervaloMedioSegundos());
  }
}
