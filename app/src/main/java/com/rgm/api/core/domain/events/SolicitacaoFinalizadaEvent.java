package com.rgm.api.core.domain.events;

import static com.rgm.api.core.domain.validation.DomainValidations.requireNonNull;

import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento de dominio publicado quando uma solicitacao atinge estado terminal (CONCLUIDA ou
 * CANCELADA). Usado para disparar o recalculo de Modelo.temPendenciaAberta.
 */
public final class SolicitacaoFinalizadaEvent {

  private final UUID solicitacaoId;
  private final UUID modeloId;
  private final StatusSolicitacao statusFinal;
  private final Instant ocorridoEm;

  public SolicitacaoFinalizadaEvent(
      final UUID solicitacaoId,
      final UUID modeloId,
      final StatusSolicitacao statusFinal,
      final Instant ocorridoEm) {
    this.solicitacaoId = requireNonNull(solicitacaoId, "solicitacaoId");
    this.modeloId = requireNonNull(modeloId, "modeloId");
    this.statusFinal = requireNonNull(statusFinal, "statusFinal");
    this.ocorridoEm = requireNonNull(ocorridoEm, "ocorridoEm");

    if (!statusFinal.isTerminal()) {
      throw new IllegalArgumentException(
          "SolicitacaoFinalizadaEvent requer status terminal, recebido: " + statusFinal);
    }
  }

  public UUID getSolicitacaoId() {
    return solicitacaoId;
  }

  public UUID getModeloId() {
    return modeloId;
  }

  public StatusSolicitacao getStatusFinal() {
    return statusFinal;
  }

  public Instant getOcorridoEm() {
    return ocorridoEm;
  }
}
