package com.rgm.api.core.application.usecases.modelo;

import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.events.SolicitacaoFinalizadaEvent;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SolicitacaoFinalizadaListenerTest {

  private RecalcularPendenciaUseCase recalcularPendenciaUseCase;
  private SolicitacaoFinalizadaListener listener;

  @BeforeEach
  void setUp() {
    recalcularPendenciaUseCase = mock(RecalcularPendenciaUseCase.class);
    listener = new SolicitacaoFinalizadaListener(recalcularPendenciaUseCase);
  }

  @Test
  void deveDelegarParaRecalcularPendencia() {
    final UUID modeloId = UUID.randomUUID();
    final UUID solicitacaoId = UUID.randomUUID();
    final SolicitacaoFinalizadaEvent event =
        new SolicitacaoFinalizadaEvent(
            solicitacaoId, modeloId, StatusSolicitacao.CONCLUIDA, Instant.now());

    listener.onSolicitacaoFinalizada(event);

    verify(recalcularPendenciaUseCase).execute(modeloId);
  }

  @Test
  void deveRecalcularParaCadaEventoRecebido() {
    final UUID modeloId1 = UUID.randomUUID();
    final UUID modeloId2 = UUID.randomUUID();

    listener.onSolicitacaoFinalizada(
        new SolicitacaoFinalizadaEvent(
            UUID.randomUUID(), modeloId1, StatusSolicitacao.CONCLUIDA, Instant.now()));
    listener.onSolicitacaoFinalizada(
        new SolicitacaoFinalizadaEvent(
            UUID.randomUUID(), modeloId2, StatusSolicitacao.CANCELADA, Instant.now()));

    verify(recalcularPendenciaUseCase).execute(modeloId1);
    verify(recalcularPendenciaUseCase).execute(modeloId2);
  }
}
