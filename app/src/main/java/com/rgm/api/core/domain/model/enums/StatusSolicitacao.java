package com.rgm.api.core.domain.model.enums;

/** Status do fluxo Kanban de uma solicitacao. */
public enum StatusSolicitacao {
  A_FAZER,
  EM_ANDAMENTO,
  EM_VALIDACAO,
  CONCLUIDA,
  CANCELADA;

  /** Retorna se o status e terminal (nao permite avanco). */
  public boolean isTerminal() {
    return this == CONCLUIDA || this == CANCELADA;
  }

  /** Retorna se o status e nao-terminal (pendencia aberta). */
  public boolean isNaoTerminal() {
    return !isTerminal();
  }

  /** Regras gerais de transicao do Kanban. */
  public boolean canTransitionTo(final StatusSolicitacao to) {
    if (to == null) {
      return false;
    }
    if (this == to) {
      return false;
    }
    return switch (this) {
      case A_FAZER -> to == EM_ANDAMENTO || to == CANCELADA;
      case EM_ANDAMENTO -> to == EM_VALIDACAO || to == CANCELADA;
      case EM_VALIDACAO -> to == EM_ANDAMENTO || to == CONCLUIDA || to == CANCELADA;
      case CONCLUIDA, CANCELADA -> false;
    };
  }

  /** Retorna se a prioridade e obrigatoria neste status. */
  public boolean exigePrioridade() {
    return this == EM_ANDAMENTO || this == EM_VALIDACAO || this == CONCLUIDA;
  }
}
