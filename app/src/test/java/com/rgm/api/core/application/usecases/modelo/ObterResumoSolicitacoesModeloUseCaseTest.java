package com.rgm.api.core.application.usecases.modelo;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.ResumoSolicitacoesModelo;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ObterResumoSolicitacoesModeloUseCaseTest {

  private ModeloRepository modeloRepository;
  private SolicitacaoRepository solicitacaoRepository;
  private ObterResumoSolicitacoesModeloUseCase useCase;

  @BeforeEach
  void setUp() {
    modeloRepository = mock(ModeloRepository.class);
    solicitacaoRepository = mock(SolicitacaoRepository.class);
    useCase = new ObterResumoSolicitacoesModeloUseCase(modeloRepository, solicitacaoRepository);
  }

  @Test
  void shouldDevolverOResumoDoRepositorioWhenModeloExiste() {
    // Arrange
    final UUID modeloId = UUID.randomUUID();
    final var esperado = new ResumoSolicitacoesModelo(6, 2, 3, 1, 259200.0, 864000.0);
    when(modeloRepository.findById(modeloId)).thenReturn(Optional.of(mock(Modelo.class)));
    when(solicitacaoRepository.resumirPorModelo(modeloId)).thenReturn(esperado);

    // Act
    final ResumoSolicitacoesModelo resultado = useCase.execute(modeloId);

    // Assert
    assertSame(esperado, resultado);
    verify(modeloRepository, times(1)).findById(modeloId);
    verify(solicitacaoRepository, times(1)).resumirPorModelo(modeloId);
    verifyNoMoreInteractions(modeloRepository, solicitacaoRepository);
  }

  @Test
  void shouldResponderNaoEncontradoWhenModeloNaoExiste() {
    // Arrange
    final UUID modeloId = UUID.randomUUID();
    when(modeloRepository.findById(modeloId)).thenReturn(Optional.empty());

    // Act
    final var erro =
        assertThrows(RecursoNaoEncontradoException.class, () -> useCase.execute(modeloId));

    // Assert
    assertSame(RecursoNaoEncontradoException.class, erro.getClass());
    verify(modeloRepository, times(1)).findById(modeloId);
    verifyNoInteractions(solicitacaoRepository);
  }
}
