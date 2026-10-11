package com.rgm.api.core.application.usecases.solicitacao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.rgm.api.core.domain.ports.repositories.GranularidadeSerie;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RotuloDeSerieTest {

  @Test
  void shouldFormatarDiaEMesWhenGranularidadeEDia() {
    // Arrange
    final LocalDate periodo = LocalDate.of(2026, 9, 5);

    // Act
    final String rotulo = RotuloDeSerie.de(periodo, GranularidadeSerie.DIA);

    // Assert
    assertEquals("05/09", rotulo);
  }

  @Test
  void shouldUsarONumeroDaSemanaIsoWhenGranularidadeESemana() {
    // Arrange
    final LocalDate segundaDaSemana37 = LocalDate.of(2026, 9, 7);

    // Act
    final String rotulo = RotuloDeSerie.de(segundaDaSemana37, GranularidadeSerie.SEMANA);

    // Assert
    assertEquals("Sem 37", rotulo);
  }

  @Test
  void shouldUsarASemanaDoAnoIsoWhenPrimeiroDiaDoAnoPertenceASemanaDoAnoAnterior() {
    // Arrange
    final LocalDate primeiroDeJaneiroDe2027 = LocalDate.of(2027, 1, 1);

    // Act
    final String rotulo = RotuloDeSerie.de(primeiroDeJaneiroDe2027, GranularidadeSerie.SEMANA);

    // Assert
    assertEquals("Sem 53", rotulo);
  }
}
