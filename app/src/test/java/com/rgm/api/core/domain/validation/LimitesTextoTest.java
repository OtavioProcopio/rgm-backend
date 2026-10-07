package com.rgm.api.core.domain.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LimitesTextoTest {

  private static Stream<Arguments> limitesDaEspecificacao() {
    return Stream.of(
        Arguments.of("titulo da solicitacao", LimitesTexto.SOLICITACAO_TITULO, 255),
        Arguments.of("descricao da solicitacao", LimitesTexto.SOLICITACAO_DESCRICAO, 2000),
        Arguments.of("comentario e motivos", LimitesTexto.COMENTARIO, 2000),
        Arguments.of("codigo do modelo", LimitesTexto.MODELO_CODIGO, 100),
        Arguments.of("descricao do modelo", LimitesTexto.MODELO_DESCRICAO, 255),
        Arguments.of("maquina do modelo", LimitesTexto.MODELO_MAQUINA, 255),
        Arguments.of("observacoes do modelo", LimitesTexto.MODELO_OBSERVACOES, 2000),
        Arguments.of("codigo do modelo pretendido", LimitesTexto.MODELO_PRETENDIDO_CODIGO, 50),
        Arguments.of("maquina do modelo pretendido", LimitesTexto.MODELO_PRETENDIDO_MAQUINA, 100),
        Arguments.of(
            "observacoes do modelo pretendido", LimitesTexto.MODELO_PRETENDIDO_OBSERVACOES, 2000),
        Arguments.of("nome da maquina", LimitesTexto.MAQUINA_NOME, 255),
        Arguments.of("nome do usuario", LimitesTexto.USUARIO_NOME, 255),
        Arguments.of("e-mail do usuario", LimitesTexto.USUARIO_EMAIL, 255));
  }

  @ParameterizedTest(name = "{0}: {2}")
  @MethodSource("limitesDaEspecificacao")
  void shouldMatchTheSpecificationWhenLimitIsRead(
      final String campo, final int limite, final int esperado) {
    // Arrange
    final String descricao = "limite de " + campo;

    // Act
    final int lido = limite;

    // Assert
    assertEquals(esperado, lido, descricao);
  }

  @Test
  void shouldNameThePlaceholderOfTheLimitWhenMessageIsRead() {
    // Arrange
    final String esperada = "deve ter no máximo {max} caracteres";

    // Act
    final String mensagem = LimitesTexto.MENSAGEM;

    // Assert
    assertEquals(esperada, mensagem);
  }
}
