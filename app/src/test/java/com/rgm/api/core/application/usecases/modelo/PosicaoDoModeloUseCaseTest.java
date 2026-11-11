package com.rgm.api.core.application.usecases.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.ports.repositories.FiltrosDeModelos;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.PosicaoDoModelo;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PosicaoDoModeloUseCaseTest {

  private static final FiltrosDeModelos FILTROS =
      new FiltrosDeModelos(true, null, null, false, null, "RESINA", null);

  private ModeloRepository repo;
  private PosicaoDoModeloUseCase useCase;

  @BeforeEach
  void setUp() {
    repo = mock(ModeloRepository.class);
    useCase = new PosicaoDoModeloUseCase(repo);
  }

  @Test
  void shouldGiveThePositionOfAnExistingModel() {
    // Arrange
    final UUID id = UUID.randomUUID();
    final Instant t = Instant.now();
    when(repo.findById(id))
        .thenReturn(
            Optional.of(
                new Modelo(
                    id, "A-1", 1, "d", null, null, null, true, "FBOX", "RESINA", false, t, t)));
    when(repo.posicaoDe(id, FILTROS)).thenReturn(new PosicaoDoModelo(33, 45));

    // Act
    final var posicao = useCase.execute(id, FILTROS);

    // Assert
    assertEquals(33, posicao.posicao());
    assertEquals(45, posicao.total());
  }

  @Test
  void shouldFailAndNotCalculateWhenTheModelDoesNotExist() {
    // Arrange
    final UUID id = UUID.randomUUID();
    when(repo.findById(id)).thenReturn(Optional.empty());

    // Act + Assert
    assertThrows(RecursoNaoEncontradoException.class, () -> useCase.execute(id, FILTROS));
    verify(repo, never()).posicaoDe(any(), any());
  }
}
