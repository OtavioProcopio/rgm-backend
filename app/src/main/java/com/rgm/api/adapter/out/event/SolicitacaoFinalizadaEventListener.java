package com.rgm.api.adapter.out.event;

import com.rgm.api.core.application.usecases.modelo.SolicitacaoFinalizadaListener;
import com.rgm.api.core.domain.events.SolicitacaoFinalizadaEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SolicitacaoFinalizadaEventListener {

  private final SolicitacaoFinalizadaListener listener;

  public SolicitacaoFinalizadaEventListener(final SolicitacaoFinalizadaListener listener) {
    this.listener = listener;
  }

  @EventListener
  public void handle(final SolicitacaoFinalizadaEvent event) {
    listener.onSolicitacaoFinalizada(event);
  }
}
