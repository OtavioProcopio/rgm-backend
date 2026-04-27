package com.rgm.api.core.domain.exceptions;

/** Erro de regra de negocio (estado/transicao/autorizacao etc.). */
public final class BusinessRuleException extends DomainException {

  public BusinessRuleException(final String message) {
    super(message);
  }
}
