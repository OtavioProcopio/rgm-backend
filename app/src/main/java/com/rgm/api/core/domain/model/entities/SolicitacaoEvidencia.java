package com.rgm.api.core.domain.model.entities;

import static com.rgm.api.core.domain.validation.DomainValidations.requireNonNull;

import java.util.UUID;

/** Relacao N:N entre Solicitacao e Evidencia. */
public final class SolicitacaoEvidencia {

  private final UUID solicitacaoId;
  private final UUID evidenciaId;

  public SolicitacaoEvidencia(final UUID solicitacaoId, final UUID evidenciaId) {
    this.solicitacaoId = requireNonNull(solicitacaoId, "solicitacaoId");
    this.evidenciaId = requireNonNull(evidenciaId, "evidenciaId");
  }

  public UUID getSolicitacaoId() {
    return solicitacaoId;
  }

  public UUID getEvidenciaId() {
    return evidenciaId;
  }
}
