package com.rgm.api.core.domain.exceptions;

/** Erro de validacao de dados (formato/obrigatoriedade). */
public final class ValidationException extends DomainException {

  public ValidationException(final String message) {
    super(message);
  }
}
