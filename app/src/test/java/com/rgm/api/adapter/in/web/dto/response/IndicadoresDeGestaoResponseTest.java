package com.rgm.api.adapter.in.web.dto.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.rgm.api.core.application.usecases.solicitacao.ObterIndicadoresDeGestaoUseCase.Backlog;
import com.rgm.api.core.application.usecases.solicitacao.ObterIndicadoresDeGestaoUseCase.Carga;
import com.rgm.api.core.application.usecases.solicitacao.ObterIndicadoresDeGestaoUseCase.Devolucao;
import com.rgm.api.core.application.usecases.solicitacao.ObterIndicadoresDeGestaoUseCase.Etapa;
import com.rgm.api.core.application.usecases.solicitacao.ObterIndicadoresDeGestaoUseCase.Etapas;
import com.rgm.api.core.application.usecases.solicitacao.ObterIndicadoresDeGestaoUseCase.Output;
import com.rgm.api.core.application.usecases.solicitacao.ObterIndicadoresDeGestaoUseCase.Prazo;
import com.rgm.api.core.application.usecases.solicitacao.ObterIndicadoresDeGestaoUseCase.Resolucao;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class IndicadoresDeGestaoResponseTest {

  @Test
  void shouldCopyEveryBlockWhenBuiltFromTheUseCaseOutput() {
    // Arrange
    final Etapa etapa = new Etapa(0, null, null);
    final Output output =
        new Output(
            30,
            Instant.parse("2026-09-12T03:00:00Z"),
            Instant.parse("2026-10-12T03:00:00Z"),
            new Prazo(0, 0, null, 0, List.of()),
            new Resolucao(0, null, null),
            new Backlog(1, 1, 0, 0, 1),
            new Etapas(etapa, etapa, etapa),
            new Devolucao(0, 0, null),
            List.of(),
            List.of(),
            new Carga(0, List.of()));

    // Act
    final IndicadoresDeGestaoResponse response = IndicadoresDeGestaoResponse.from(output);

    // Assert
    assertEquals(30, response.dias());
    assertEquals(output.inicio(), response.inicio());
    assertSame(output.backlog(), response.backlog());
    assertSame(output.etapas(), response.etapas());
    assertSame(output.carga(), response.carga());
  }
}
