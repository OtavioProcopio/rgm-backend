package com.rgm.api.core.domain.ports.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ResumoSolicitacoesModeloTest {

  private static final Instant PRIMEIRA = Instant.parse("2026-09-01T12:00:00Z");

  @Test
  void shouldDividirOTempoEntreAPrimeiraEAUltimaAberturaPelosIntervalosWhenHaDuasOuMais() {
    // Arrange
    final long total = 4;
    final Duration intervaloEsperado = Duration.ofDays(10);
    final Instant ultima = PRIMEIRA.plus(intervaloEsperado.multipliedBy(total - 1));

    // Act
    final var resumo = ResumoSolicitacoesModelo.of(total, 1, 2, 1, 259200.0, PRIMEIRA, ultima);

    // Assert
    assertEquals(intervaloEsperado.getSeconds(), resumo.intervaloMedioSegundos());
  }

  @Test
  void shouldManterContagensETempoDeResolucaoWhenMontaOResumo() {
    // Arrange
    final Double tempoMedio = 259200.0;

    // Act
    final var resumo =
        ResumoSolicitacoesModelo.of(6, 2, 3, 1, tempoMedio, PRIMEIRA, PRIMEIRA.plusSeconds(50));

    // Assert
    assertEquals(new ResumoSolicitacoesModelo(6, 2, 3, 1, tempoMedio, 10.0), resumo);
  }

  @Test
  void shouldDeixarIntervaloVazioWhenHaUmaUnicaSolicitacao() {
    // Arrange
    final long total = 1;

    // Act
    final var resumo = ResumoSolicitacoesModelo.of(total, 0, 1, 0, 172800.0, PRIMEIRA, PRIMEIRA);

    // Assert
    assertNull(resumo.intervaloMedioSegundos());
  }

  @Test
  void shouldDeixarIntervaloVazioWhenNaoHaSolicitacao() {
    // Arrange
    final long total = 0;

    // Act
    final var resumo = ResumoSolicitacoesModelo.of(total, 0, 0, 0, null, null, null);

    // Assert
    assertNull(resumo.intervaloMedioSegundos());
  }

  @Test
  void shouldDeixarIntervaloVazioWhenFaltaUmaDasDatas() {
    // Arrange
    final long total = 2;

    // Act
    final var semUltima = ResumoSolicitacoesModelo.of(total, 2, 0, 0, null, PRIMEIRA, null);

    // Assert
    assertNull(semUltima.intervaloMedioSegundos());
  }
}
