package com.rgm.api.core.application.usecases.modelo;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rgm.api.core.domain.ports.repositories.ContagensDeModelos;
import com.rgm.api.core.domain.ports.repositories.FiltrosDeModelos;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContarModelosUseCaseTest {

  @Test
  void shouldAskTheRepositoryForTheCountsOfTheSameFilters() {
    // Arrange
    final ModeloRepository repo = mock(ModeloRepository.class);
    final var filtros = new FiltrosDeModelos(true, null, "FBOX", true, null, "RESINA", "carcaca");
    final var contagens =
        new ContagensDeModelos(
            3, List.of(new ContagensDeModelos.Quantidade("RESINA", 3)), List.of());
    when(repo.contar(filtros)).thenReturn(contagens);

    // Act
    final var resultado = new ContarModelosUseCase(repo).execute(filtros);

    // Assert
    assertSame(contagens, resultado);
    verify(repo).contar(filtros);
  }
}
