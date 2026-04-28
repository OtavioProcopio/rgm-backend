package com.rgm.api.core.application.usecases.modelo;

import com.rgm.api.core.domain.events.SolicitacaoFinalizadaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener que reage ao evento SolicitacaoFinalizadaEvent para recalcular
 * Modelo.temPendenciaAberta. A implementacao concreta (adapter) deve delegar para
 * RecalcularPendenciaUseCase.
 */
public final class SolicitacaoFinalizadaListener {
  private static final Logger log = LoggerFactory.getLogger(SolicitacaoFinalizadaListener.class);

  private final RecalcularPendenciaUseCase recalcularPendenciaUseCase;

  public SolicitacaoFinalizadaListener(
      final RecalcularPendenciaUseCase recalcularPendenciaUseCase) {
    this.recalcularPendenciaUseCase = recalcularPendenciaUseCase;
  }

  public void onSolicitacaoFinalizada(final SolicitacaoFinalizadaEvent event) {
    log.info("SolicitacaoFinalizadaListener.onSolicitacaoFinalizada iniciado");
    recalcularPendenciaUseCase.execute(event.getModeloId());
  }
}
