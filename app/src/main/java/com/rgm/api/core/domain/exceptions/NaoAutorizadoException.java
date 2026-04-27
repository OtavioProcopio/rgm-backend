package com.rgm.api.core.domain.exceptions;

/** Erro de autorizacao (perfil sem permissao para a operacao). */
public final class NaoAutorizadoException extends DomainException {

  public NaoAutorizadoException(final String message) {
    super(message);
  }
}
