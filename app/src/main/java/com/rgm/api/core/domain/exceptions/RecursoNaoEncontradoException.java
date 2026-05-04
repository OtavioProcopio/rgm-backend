package com.rgm.api.core.domain.exceptions;

/** Erro quando um recurso nao e encontrado (HTTP 404). */
public final class RecursoNaoEncontradoException extends DomainException {

  public RecursoNaoEncontradoException(final String message) {
    super(message);
  }
}
