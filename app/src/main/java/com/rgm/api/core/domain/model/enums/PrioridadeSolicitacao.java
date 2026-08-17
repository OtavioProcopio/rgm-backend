package com.rgm.api.core.domain.model.enums;

/** Prioridade da solicitacao no Kanban. */
public enum PrioridadeSolicitacao {
  BAIXA(168),
  MEDIA(72),
  ALTA(24),
  URGENTE(4);

  private final int slaHoras;

  PrioridadeSolicitacao(final int slaHoras) {
    this.slaHoras = slaHoras;
  }

  /** Prazo de SLA, em horas, contado a partir da abertura da solicitacao. */
  public int slaHoras() {
    return slaHoras;
  }
}
