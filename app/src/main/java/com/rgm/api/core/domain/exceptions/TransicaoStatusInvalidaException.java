package com.rgm.api.core.domain.exceptions;

import com.rgm.api.core.domain.model.enums.StatusSolicitacao;

/** Erro de transicao de status invalida no fluxo Kanban. */
public final class TransicaoStatusInvalidaException extends DomainException {

  private final StatusSolicitacao de;
  private final StatusSolicitacao para;

  public TransicaoStatusInvalidaException(
      final StatusSolicitacao de, final StatusSolicitacao para) {
    super("Transicao de status invalida: " + de.name() + " -> " + para.name());
    this.de = de;
    this.para = para;
  }

  public StatusSolicitacao getDe() {
    return de;
  }

  public StatusSolicitacao getPara() {
    return para;
  }
}
