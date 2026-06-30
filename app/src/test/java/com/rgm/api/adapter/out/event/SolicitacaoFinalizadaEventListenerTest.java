package com.rgm.api.adapter.out.event;

import static org.mockito.Mockito.verify;

import com.rgm.api.core.application.usecases.modelo.SolicitacaoFinalizadaListener;
import com.rgm.api.core.domain.events.SolicitacaoFinalizadaEvent;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SolicitacaoFinalizadaEventListenerTest {

  @Mock private SolicitacaoFinalizadaListener listener;

  @InjectMocks private SolicitacaoFinalizadaEventListener eventListener;

  @Test
  void deveDelegarEventoParaOListener() {
    final SolicitacaoFinalizadaEvent event =
        new SolicitacaoFinalizadaEvent(
            UUID.randomUUID(), UUID.randomUUID(), StatusSolicitacao.CONCLUIDA, Instant.now());

    eventListener.handle(event);

    verify(listener).onSolicitacaoFinalizada(event);
  }
}
