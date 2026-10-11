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
    final long manutencoes = 4;
    final Duration intervaloEsperado = Duration.ofDays(10);
    final Instant ultima = PRIMEIRA.plus(intervaloEsperado.multipliedBy(manutencoes - 1));

    // Act
    final var resumo =
        ResumoSolicitacoesModelo.of(manutencoes, 1, 2, 1, 259200.0, manutencoes, PRIMEIRA, ultima);

    // Assert
    assertEquals(intervaloEsperado.getSeconds(), resumo.intervaloMedioSegundos());
  }

  @Test
  void shouldDividirPelaContagemDeManutencoesWhenOTotalInclueCriacao() {
    // Arrange
    final long total = 5;
    final long manutencoes = 3;
    final Instant ultima = PRIMEIRA.plusSeconds(100);

    // Act
    final var resumo =
        ResumoSolicitacoesModelo.of(total, 0, 5, 0, 60.0, manutencoes, PRIMEIRA, ultima);

    // Assert
    assertEquals(50.0, resumo.intervaloMedioSegundos());
  }

  @Test
  void shouldContarACriacaoNoTotalWhenMontaOResumo() {
    // Arrange
    final long total = 5;
    final long manutencoes = 4;

    // Act
    final var resumo =
        ResumoSolicitacoesModelo.of(
            total, 0, 5, 0, 60.0, manutencoes, PRIMEIRA, PRIMEIRA.plusSeconds(30));

    // Assert
    assertEquals(total, resumo.total());
  }

  @Test
  void shouldManterContagensETempoDeResolucaoWhenMontaOResumo() {
    // Arrange
    final Double tempoMedio = 259200.0;

    // Act
    final var resumo =
        ResumoSolicitacoesModelo.of(6, 2, 3, 1, tempoMedio, 6, PRIMEIRA, PRIMEIRA.plusSeconds(50));

    // Assert
    assertEquals(new ResumoSolicitacoesModelo(6, 2, 3, 1, tempoMedio, 10.0), resumo);
  }

  @Test
  void shouldDeixarIntervaloVazioWhenHaUmaUnicaManutencao() {
    // Arrange
    final long manutencoes = 1;

    // Act
    final var resumo =
        ResumoSolicitacoesModelo.of(3, 0, 1, 0, 172800.0, manutencoes, PRIMEIRA, PRIMEIRA);

    // Assert
    assertNull(resumo.intervaloMedioSegundos());
  }

  @Test
  void shouldDeixarTempoEIntervaloVaziosWhenModeloTemSoCriacao() {
    // Arrange
    final long total = 2;

    // Act
    final var resumo = ResumoSolicitacoesModelo.of(total, 0, 2, 0, null, 0, null, null);

    // Assert
    assertEquals(new ResumoSolicitacoesModelo(2, 0, 2, 0, null, null), resumo);
  }

  @Test
  void shouldDeixarIntervaloVazioWhenNaoHaSolicitacao() {
    // Arrange
    final long total = 0;

    // Act
    final var resumo = ResumoSolicitacoesModelo.of(total, 0, 0, 0, null, 0, null, null);

    // Assert
    assertNull(resumo.intervaloMedioSegundos());
  }

  @Test
  void shouldDeixarIntervaloVazioWhenFaltaUmaDasDatas() {
    // Arrange
    final long manutencoes = 2;

    // Act
    final var semUltima =
        ResumoSolicitacoesModelo.of(manutencoes, 2, 0, 0, null, manutencoes, PRIMEIRA, null);

    // Assert
    assertNull(semUltima.intervaloMedioSegundos());
  }
}
