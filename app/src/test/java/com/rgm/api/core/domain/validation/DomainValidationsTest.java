package com.rgm.api.core.domain.validation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DomainValidationsTest {

  @Test
  void requireNonNullDeveRetornarValor() {
    final String valor = "x";
    assertSame(valor, DomainValidations.requireNonNull(valor, "campo"));
  }

  @Test
  void requireNonNullDeveFalharComNulo() {
    final NullPointerException ex =
        assertThrows(
            NullPointerException.class, () -> DomainValidations.requireNonNull(null, "campo"));
    assertEquals("campo nao pode ser nulo", ex.getMessage());
  }

  @Test
  void requireNonBlankDeveRetornarTrimmed() {
    assertEquals("abc", DomainValidations.requireNonBlank("  abc  ", "campo"));
  }

  @Test
  void requireNonBlankDeveFalharComNulo() {
    final NullPointerException ex =
        assertThrows(
            NullPointerException.class, () -> DomainValidations.requireNonBlank(null, "campo"));
    assertEquals("campo nao pode ser nulo", ex.getMessage());
  }

  @Test
  void requireNonBlankDeveFalharComVazio() {
    final IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> DomainValidations.requireNonBlank("   ", "campo"));
    assertEquals("campo nao pode ser vazio", ex.getMessage());
  }

  @Test
  void optionalTrimToNullDeveRetornarNullComNulo() {
    assertNull(DomainValidations.optionalTrimToNull(null));
  }

  @Test
  void optionalTrimToNullDeveRetornarNullComVazio() {
    assertNull(DomainValidations.optionalTrimToNull("   "));
  }

  @Test
  void optionalTrimToNullDeveRetornarTrimmed() {
    assertEquals("abc", DomainValidations.optionalTrimToNull("  abc  "));
  }

  @Test
  void requirePositiveOrZeroDeveAceitarZero() {
    assertEquals(0, DomainValidations.requirePositiveOrZero(0, "campo"));
  }

  @Test
  void requirePositiveOrZeroDeveAceitarPositivo() {
    assertEquals(5, DomainValidations.requirePositiveOrZero(5, "campo"));
  }

  @Test
  void requirePositiveOrZeroDeveFalharComNegativo() {
    final IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> DomainValidations.requirePositiveOrZero(-1, "campo"));
    assertEquals("campo deve ser >= 0", ex.getMessage());
  }

  @Test
  void requirePositiveDeveAceitarPositivo() {
    assertEquals(1, DomainValidations.requirePositive(1, "campo"));
  }

  @Test
  void requirePositiveDeveFalharComZero() {
    final IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> DomainValidations.requirePositive(0, "campo"));
    assertEquals("campo deve ser > 0", ex.getMessage());
  }

  @Test
  void requirePositiveDeveFalharComNegativo() {
    assertThrows(
        IllegalArgumentException.class, () -> DomainValidations.requirePositive(-3, "campo"));
  }
}
