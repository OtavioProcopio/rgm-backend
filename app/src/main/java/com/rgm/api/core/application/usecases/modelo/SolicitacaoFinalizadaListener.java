package com.rgm.api.core.application.usecases.modelo;

import com.rgm.api.core.domain.events.SolicitacaoFinalizadaEvent;

/**
 * Listener que reage ao evento SolicitacaoFinalizadaEvent para recalcular
 * Modelo.temPendenciaAberta. A implementacao concreta (adapter) deve delegar para
 * RecalcularPendenciaUseCase.
 */
public final class SolicitacaoFinalizadaListener {

  private final RecalcularPendenciaUseCase recalcularPendenciaUseCase;

  public SolicitacaoFinalizadaListener(
      final RecalcularPendenciaUseCase recalcularPendenciaUseCase) {
    this.recalcularPendenciaUseCase = recalcularPendenciaUseCase;
  }

  public void onSolicitacaoFinalizada(final SolicitacaoFinalizadaEvent event) {
    recalcularPendenciaUseCase.execute(event.getModeloId());
  }
}
