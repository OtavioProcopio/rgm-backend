package com.rgm.api.core.domain.validation;

import java.util.Objects;

/** Utilitarios de validacao para o dominio (sem dependencias externas). */
public final class DomainValidations {

  private DomainValidations() {}

  /** Exige nao-nulo. */
  public static <T> T requireNonNull(final T value, final String fieldName) {
    return Objects.requireNonNull(value, fieldName + " nao pode ser nulo");
  }

  /** Exige string nao-vazia (trim). */
  public static String requireNonBlank(final String value, final String fieldName) {
    requireNonNull(value, fieldName);
    final String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException(fieldName + " nao pode ser vazio");
    }
    return trimmed;
  }

  /** Valida string opcional: se presente, retorna trimmed; se vazia, retorna null. */
  public static String optionalTrimToNull(final String value) {
    if (value == null) {
      return null;
    }
    final String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  /** Exige inteiro >= 0. */
  public static int requirePositiveOrZero(final int value, final String fieldName) {
    if (value < 0) {
      throw new IllegalArgumentException(fieldName + " deve ser >= 0");
    }
    return value;
  }

  /** Exige inteiro > 0. */
  public static int requirePositive(final int value, final String fieldName) {
    if (value <= 0) {
      throw new IllegalArgumentException(fieldName + " deve ser > 0");
    }
    return value;
  }
}
