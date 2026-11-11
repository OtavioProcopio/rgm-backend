package com.rgm.api.core.application.usecases.modelo;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.ports.repositories.FiltrosDeModelos;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.OrdemDeModelos;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListarModelosUseCaseTest {

  private ModeloRepository modeloRepository;
  private ListarModelosUseCase useCase;

  @BeforeEach
  void setUp() {
    modeloRepository = mock(ModeloRepository.class);
    useCase = new ListarModelosUseCase(modeloRepository);
  }

  @Test
  void execute_comFiltrosNulos_deveChamarFindAll() {
    // Arrange
    final ListarModelosUseCase.Input input =
        new ListarModelosUseCase.Input(null, null, null, null, 0, 20);
    final PageResult<Modelo> expectedPage = new PageResult<>(List.of(), 0, 20, 0, 0);
    when(modeloRepository.findAll(anyInt(), anyInt())).thenReturn(expectedPage);

    // Act
    final PageResult<Modelo> result = useCase.execute(input);

    // Assert
    assertNotNull(result);
    verify(modeloRepository).findAll(0, 20);
  }

  @Test
  void execute_comAtivoNaoNulo_deveChamarFindByFilters() {
    // Arrange
    final ListarModelosUseCase.Input input =
        new ListarModelosUseCase.Input(true, null, null, null, 0, 20);
    final PageResult<Modelo> expectedPage = new PageResult<>(List.of(), 0, 20, 0, 0);
    when(modeloRepository.findByFilters(any(), any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(expectedPage);

    // Act
    final PageResult<Modelo> result = useCase.execute(input);

    // Assert
    assertNotNull(result);
    verify(modeloRepository).findByFilters(true, null, null, null, 0, 20);
  }

  @Test
  void execute_comCodigoNaoNulo_deveChamarFindByFilters() {
    // Arrange
    final ListarModelosUseCase.Input input =
        new ListarModelosUseCase.Input(null, "MOD-01", null, null, 0, 20);
    final PageResult<Modelo> expectedPage = new PageResult<>(List.of(), 0, 20, 0, 0);
    when(modeloRepository.findByFilters(any(), any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(expectedPage);

    // Act
    final PageResult<Modelo> result = useCase.execute(input);

    // Assert
    assertNotNull(result);
    verify(modeloRepository).findByFilters(null, "MOD-01", null, null, 0, 20);
  }

  @Test
  void execute_comMaquinaNaoNula_deveChamarFindByFilters() {
    // Arrange
    final ListarModelosUseCase.Input input =
        new ListarModelosUseCase.Input(null, null, "INJ-01", null, 0, 20);
    final PageResult<Modelo> expectedPage = new PageResult<>(List.of(), 0, 20, 0, 0);
    when(modeloRepository.findByFilters(any(), any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(expectedPage);

    // Act
    final PageResult<Modelo> result = useCase.execute(input);

    // Assert
    assertNotNull(result);
    verify(modeloRepository).findByFilters(null, null, "INJ-01", null, 0, 20);
  }

  @Test
  void execute_comDescricaoNaoNula_deveChamarFindByFilters() {
    // Arrange
    final ListarModelosUseCase.Input input =
        new ListarModelosUseCase.Input(null, null, null, "Descrição teste", 0, 20);
    final PageResult<Modelo> expectedPage = new PageResult<>(List.of(), 0, 20, 0, 0);
    when(modeloRepository.findByFilters(any(), any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(expectedPage);

    // Act
    final PageResult<Modelo> result = useCase.execute(input);

    // Assert
    assertNotNull(result);
    verify(modeloRepository).findByFilters(null, null, null, "Descrição teste", 0, 20);
  }

  @Test
  void shouldUseTheCatalogWhenAnyCatalogFilterOrTheCodeOrderIsAsked() {
    // Arrange
    final PageResult<Modelo> pagina = new PageResult<>(List.of(), 0, 20, 0, 0);
    when(modeloRepository.buscarCatalogo(any(), any(), anyInt(), anyInt())).thenReturn(pagina);
    final var entradas =
        List.of(
            new ListarModelosUseCase.Input(
                null, null, null, null, "RESINA", null, false, OrdemDeModelos.CRIACAO, 0, 20),
            new ListarModelosUseCase.Input(
                null, null, null, null, null, "carcaca", false, OrdemDeModelos.CRIACAO, 0, 20),
            new ListarModelosUseCase.Input(
                null, null, "FBOX", null, null, null, true, OrdemDeModelos.CRIACAO, 0, 20),
            new ListarModelosUseCase.Input(
                null, null, null, null, null, null, false, OrdemDeModelos.CODIGO, 0, 20));

    // Act
    entradas.forEach(useCase::execute);

    // Assert
    verify(modeloRepository, times(4)).buscarCatalogo(any(), any(), anyInt(), anyInt());
    verify(modeloRepository, never()).findAll(anyInt(), anyInt());
    verify(modeloRepository, never()).findByFilters(any(), any(), any(), any(), anyInt(), anyInt());
  }

  @Test
  void shouldHandOverEveryFilterAndTheOrderToTheRepository() {
    // Arrange
    when(modeloRepository.buscarCatalogo(any(), any(), anyInt(), anyInt()))
        .thenReturn(new PageResult<>(List.of(), 2, 20, 0, 0));
    final var input =
        new ListarModelosUseCase.Input(
            true, "MD", "FBOX", "desc", "RESINA", "carcaca", true, OrdemDeModelos.CODIGO, 2, 20);

    // Act
    useCase.execute(input);

    // Assert
    verify(modeloRepository)
        .buscarCatalogo(
            new FiltrosDeModelos(true, "MD", "FBOX", true, "desc", "RESINA", "carcaca"),
            OrdemDeModelos.CODIGO,
            2,
            20);
  }

  @Test
  void shouldKeepTheOldPathForTheOldInputWithoutCatalogParameters() {
    // Arrange
    when(modeloRepository.findByFilters(any(), any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));

    // Act
    useCase.execute(new ListarModelosUseCase.Input(true, "MD", "FBOX", "desc", 0, 20));

    // Assert
    verify(modeloRepository).findByFilters(true, "MD", "FBOX", "desc", 0, 20);
    verify(modeloRepository, never()).buscarCatalogo(any(), any(), anyInt(), anyInt());
  }
}
