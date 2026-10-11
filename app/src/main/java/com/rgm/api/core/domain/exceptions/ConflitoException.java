package com.rgm.api.core.domain.exceptions;

/** Pedido que colide com o que ja existe (ex.: identificacao de modelo repetida). */
public final class ConflitoException extends DomainException {

  public ConflitoException(final String message) {
    super(message);
  }
}
