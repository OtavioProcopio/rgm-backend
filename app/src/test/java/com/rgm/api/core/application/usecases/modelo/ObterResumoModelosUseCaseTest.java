package com.rgm.api.core.application.usecases.modelo;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.QuantidadePorMaquina;
import com.rgm.api.core.domain.ports.repositories.ResumoModelos;
import java.util.List;
import org.junit.jupiter.api.Test;

class ObterResumoModelosUseCaseTest {

  @Test
  void shouldDevolverOResumoDoRepositorioWhenConsultado() {
    // Arrange
    final ModeloRepository modeloRepository = mock(ModeloRepository.class);
    final var esperado =
        new ResumoModelos(
            7,
            5,
            2,
            2,
            List.of(new QuantidadePorMaquina("DISA", 4), new QuantidadePorMaquina("FBOX", 3)));
    when(modeloRepository.resumir()).thenReturn(esperado);
    final var useCase = new ObterResumoModelosUseCase(modeloRepository);

    // Act
    final ResumoModelos resultado = useCase.execute();

    // Assert
    assertSame(esperado, resultado);
    verify(modeloRepository, times(1)).resumir();
    verifyNoMoreInteractions(modeloRepository);
  }
}
