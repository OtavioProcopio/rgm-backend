package com.rgm.api.adapter.in.web.dto.response;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.rgm.api.core.application.usecases.solicitacao.ObterMetricasSolicitacoesUseCase;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MetricasSolicitacaoResponseTest {

  @Test
  void shouldCopyOpenDistributionsAndKeepTheOldFieldsWhenBuiltFromOutput() {
    // Arrange
    final UUID modeloId = UUID.randomUUID();
    final var output =
        new ObterMetricasSolicitacoesUseCase.Output(
            1,
            2,
            3,
            Map.of("A_FAZER", 3L),
            3,
            0,
            0,
            120,
            Map.of(modeloId, 3L),
            Map.of("REPARO", 3L),
            Map.of("SEM_PRIORIDADE", 3L));

    // Act
    final MetricasSolicitacaoResponse response = MetricasSolicitacaoResponse.from(output);

    // Assert
    assertEquals(Map.of("REPARO", 3L), response.emAbertoPorTipo());
    assertEquals(Map.of("SEM_PRIORIDADE", 3L), response.emAbertoPorPrioridade());
    assertEquals(Map.of("A_FAZER", 3L), response.solicitacoesPorStatus());
    assertEquals(120, response.tempoMedioResolucaoSegundos());
    assertEquals(Map.of(modeloId, 3L), response.solicitacoesPorModelo());
  }
}
