package com.rgm.api.core.domain.exceptions;

/** Excecao base do dominio (regras e validacoes). */
public class DomainException extends RuntimeException {

  public DomainException(final String message) {
    super(message);
  }

  public DomainException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
