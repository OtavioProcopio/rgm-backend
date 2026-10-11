package com.rgm.api.adapter.out.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;

class ContextoDoRelatorioTest {

  @Test
  void shouldSkipFiltersThatWereNotInformedWhenBuildingTheList() {
    // Arrange
    final var filtros = new ContextoDoRelatorio.Filtros();

    // Act
    final List<String> lista =
        filtros.com("Máquina", "FBOX").com("Código", null).com("Descrição", "  ").build();

    // Assert
    assertEquals(List.of("Máquina: FBOX"), lista);
  }

  @Test
  void shouldWriteEnumValuesInReadableForm() {
    // Arrange
    final var filtros = new ContextoDoRelatorio.Filtros();

    // Act
    final List<String> lista =
        filtros.comEnum("Status", "EM_ANDAMENTO").comEnum("Tipo", null).build();

    // Assert
    assertEquals(List.of("Status: Em andamento"), lista);
  }

  @Test
  void shouldSaySemFiltrosAndHaveNoWarningWhenNothingWasAppliedOrCut() {
    // Arrange
    final var contexto = ContextoDoRelatorio.semFiltros(3);

    // Act
    final String texto = contexto.filtrosEmTexto();

    // Assert
    assertEquals("Sem filtros", texto);
    assertNull(contexto.aviso());
  }

  @Test
  void shouldWarnWhenFewerRowsThanTheTotalAreShown() {
    // Arrange
    final var contexto = new ContextoDoRelatorio(List.of("Máquina: FBOX"), 5001, 5000);

    // Act
    final String aviso = contexto.aviso();

    // Assert
    assertEquals("Exibindo 5000 de 5001 — refine os filtros para ver o restante.", aviso);
    assertEquals("Filtros: Máquina: FBOX", contexto.filtrosEmTexto());
  }
}
