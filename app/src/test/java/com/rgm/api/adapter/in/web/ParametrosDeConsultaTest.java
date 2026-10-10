package com.rgm.api.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ParametrosDeConsultaTest {

  @ParameterizedTest
  @ValueSource(ints = {1, 20, 100})
  void shouldAceitarWhenTamanhoDaPaginaEstaNoIntervalo(final int size) {
    // Arrange
    final int page = 0;

    // Act
    final Executable chamada = () -> ParametrosDeConsulta.pagina(page, size);

    // Assert
    assertDoesNotThrow(chamada);
  }

  @Test
  void shouldRecusarWhenPaginaENegativa() {
    // Arrange
    final int page = -1;

    // Act
    final ValidationException erro =
        assertThrows(ValidationException.class, () -> ParametrosDeConsulta.pagina(page, 20));

    // Assert
    assertTrue(erro.getMessage().contains("'page'"));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -5, 101, 5000})
  void shouldRecusarWhenTamanhoDaPaginaEstaForaDoIntervalo(final int size) {
    // Arrange
    final int page = 0;

    // Act
    final ValidationException erro =
        assertThrows(ValidationException.class, () -> ParametrosDeConsulta.pagina(page, size));

    // Assert
    assertTrue(erro.getMessage().contains("'size'"));
    assertTrue(erro.getMessage().contains("entre 1 e 100"));
  }

  @Test
  void shouldDevolverInstanteWhenDataEstaEmIso8601() {
    // Arrange
    final String valor = "2026-10-10T12:30:00Z";

    // Act
    final Instant data = ParametrosDeConsulta.data("dataInicio", valor);

    // Assert
    assertEquals(Instant.parse(valor), data);
  }

  @Test
  void shouldDevolverNuloWhenDataNaoFoiInformada() {
    // Arrange
    final String valor = null;

    // Act
    final Instant data = ParametrosDeConsulta.data("dataInicio", valor);

    // Assert
    assertNull(data);
  }

  @ParameterizedTest
  @ValueSource(strings = {"ontem", "32/13/2026", "xx", ""})
  void shouldRecusarWhenDataNaoEstaEmIso8601(final String valor) {
    // Arrange
    final String campo = "criadaEmInicio";

    // Act
    final ValidationException erro =
        assertThrows(ValidationException.class, () -> ParametrosDeConsulta.data(campo, valor));

    // Assert
    assertTrue(erro.getMessage().contains("'criadaEmInicio'"));
    assertTrue(erro.getMessage().contains("ISO-8601"));
  }

  @Test
  void shouldDevolverConstanteWhenEnumeracaoEValida() {
    // Arrange
    final String valor = "EM_ANDAMENTO";

    // Act
    final StatusSolicitacao status =
        ParametrosDeConsulta.enumeracao("status", valor, StatusSolicitacao.class);

    // Assert
    assertEquals(StatusSolicitacao.EM_ANDAMENTO, status);
  }

  @Test
  void shouldDevolverNuloWhenEnumeracaoNaoFoiInformada() {
    // Arrange
    final String valor = null;

    // Act
    final StatusSolicitacao status =
        ParametrosDeConsulta.enumeracao("status", valor, StatusSolicitacao.class);

    // Assert
    assertNull(status);
  }

  @Test
  void shouldDizerOCampoEOsValoresAceitosWhenEnumeracaoEInvalida() {
    // Arrange
    final String valor = "INVALIDO";

    // Act
    final ValidationException erro =
        assertThrows(
            ValidationException.class,
            () -> ParametrosDeConsulta.enumeracao("status", valor, StatusSolicitacao.class));

    // Assert
    assertTrue(erro.getMessage().contains("'status'"));
    assertTrue(erro.getMessage().contains("A_FAZER"));
    assertTrue(erro.getMessage().contains("CANCELADA"));
  }

  @Test
  void shouldNaoExporClasseInternaWhenEnumeracaoEInvalida() {
    // Arrange
    final String valor = "INVALIDO";

    // Act
    final ValidationException erro =
        assertThrows(
            ValidationException.class,
            () -> ParametrosDeConsulta.enumeracao("status", valor, StatusSolicitacao.class));

    // Assert
    assertFalse(erro.getMessage().contains("com.rgm"));
    assertFalse(erro.getMessage().contains("No enum constant"));
  }

  @Test
  void shouldInformarOValorRecebidoWhenTamanhoDaPaginaEstaForaDoIntervalo() {
    // Arrange
    final int size = 5000;

    // Act
    final ValidationException erro =
        assertThrows(ValidationException.class, () -> ParametrosDeConsulta.pagina(0, size));

    // Assert
    assertTrue(erro.getMessage().contains("recebido: 5000"));
  }

  @Test
  void shouldInformarOValorRecebidoWhenPaginaENegativa() {
    // Arrange
    final int page = -3;

    // Act
    final ValidationException erro =
        assertThrows(ValidationException.class, () -> ParametrosDeConsulta.pagina(page, 20));

    // Assert
    assertTrue(erro.getMessage().contains("recebido: -3"));
  }

  @Test
  void shouldInformarOValorRecebidoWhenDataNaoEstaEmIso8601() {
    // Arrange
    final String valor = "ontem";

    // Act
    final ValidationException erro =
        assertThrows(ValidationException.class, () -> ParametrosDeConsulta.data("dataFim", valor));

    // Assert
    assertTrue(erro.getMessage().contains("recebido: ontem"));
  }

  @Test
  void shouldInformarOValorRecebidoWhenEnumeracaoEInvalida() {
    // Arrange
    final String valor = "INVALIDO";

    // Act
    final ValidationException erro =
        assertThrows(
            ValidationException.class,
            () -> ParametrosDeConsulta.enumeracao("status", valor, StatusSolicitacao.class));

    // Assert
    assertTrue(erro.getMessage().contains("recebido: INVALIDO"));
  }

  @Test
  void shouldTruncarOValorRecebidoWhenEleEMuitoLongo() {
    // Arrange
    final String valor = "x".repeat(200);

    // Act
    final ValidationException erro =
        assertThrows(ValidationException.class, () -> ParametrosDeConsulta.data("dataFim", valor));

    // Assert
    assertFalse(erro.getMessage().contains("x".repeat(51)));
    assertTrue(erro.getMessage().contains("x".repeat(50) + "…"));
  }
}
