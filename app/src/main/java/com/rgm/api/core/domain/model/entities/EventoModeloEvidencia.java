package com.rgm.api.core.domain.model.entities;

import static com.rgm.api.core.domain.validation.DomainValidations.requireNonNull;

import java.util.UUID;

/** Relacao N:N entre EventoModelo e Evidencia. */
public final class EventoModeloEvidencia {

  private final UUID eventoModeloId;
  private final UUID evidenciaId;

  public EventoModeloEvidencia(final UUID eventoModeloId, final UUID evidenciaId) {
    this.eventoModeloId = requireNonNull(eventoModeloId, "eventoModeloId");
    this.evidenciaId = requireNonNull(evidenciaId, "evidenciaId");
  }

  public UUID getEventoModeloId() {
    return eventoModeloId;
  }

  public UUID getEvidenciaId() {
    return evidenciaId;
  }
}
