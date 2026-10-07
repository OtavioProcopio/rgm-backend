package com.rgm.api.core.domain.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.rgm.api.core.domain.exceptions.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class PoliticaSenhaTest {

  private static final String MENSAGEM =
      "Senha deve ter no minimo " + PoliticaSenha.TAMANHO_MINIMO + " caracteres";

  @Test
  void shouldBeEightCharactersWhenMinimumLengthIsRead() {
    // Arrange
    final int esperado = 8;

    // Act
    final int minimo = PoliticaSenha.TAMANHO_MINIMO;

    // Assert
    assertEquals(esperado, minimo);
  }

  @Test
  void shouldRejectPasswordWhenOneCharacterBelowMinimum() {
    // Arrange
    final String senha = "a".repeat(PoliticaSenha.TAMANHO_MINIMO - 1);

    // Act
    final ValidationException erro =
        assertThrows(ValidationException.class, () -> PoliticaSenha.validar(senha));

    // Assert
    assertEquals(MENSAGEM, erro.getMessage());
  }

  @Test
  void shouldAcceptPasswordWhenExactlyAtMinimum() {
    // Arrange
    final String senha = "a".repeat(PoliticaSenha.TAMANHO_MINIMO);

    // Act
    final Runnable validacao = () -> PoliticaSenha.validar(senha);

    // Assert
    assertDoesNotThrow(validacao::run);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "            "})
  void shouldRejectPasswordWhenMissingOrBlank(final String senha) {
    // Arrange
    final String esperado = MENSAGEM;

    // Act
    final ValidationException erro =
        assertThrows(ValidationException.class, () -> PoliticaSenha.validar(senha));

    // Assert
    assertEquals(esperado, erro.getMessage());
  }
}
