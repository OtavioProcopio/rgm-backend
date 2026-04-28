package com.rgm.api.core.domain.exceptions;

/** Recurso nao encontrado (404). */
public final class RecursoNaoEncontradoException extends DomainException {

  public RecursoNaoEncontradoException(final String message) {
    super(message);
  }
}
