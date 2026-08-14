package com.rgm.api.core.application.usecases.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.FotoGaleriaModelo;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.ports.repositories.FotoGaleriaModeloRepository;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListarGaleriaModeloUseCaseTest {

  private ModeloRepository modeloRepository;
  private FotoGaleriaModeloRepository fotoGaleriaModeloRepository;
  private ListarGaleriaModeloUseCase useCase;

  @BeforeEach
  void setUp() {
    modeloRepository = mock(ModeloRepository.class);
    fotoGaleriaModeloRepository = mock(FotoGaleriaModeloRepository.class);
    useCase = new ListarGaleriaModeloUseCase(modeloRepository, fotoGaleriaModeloRepository);
  }

  @Test
  void deveListarFotosDoModelo() {
    final Modelo modelo = Modelo.criar("COD-01", "Desc", null, "FBOX", 1, Instant.now());
    final FotoGaleriaModelo foto =
        FotoGaleriaModelo.criar(
            modelo.getId(), "http://x/1.jpg", "Parte 1", true, UUID.randomUUID(), Instant.now());

    when(modeloRepository.findById(modelo.getId())).thenReturn(Optional.of(modelo));
    when(fotoGaleriaModeloRepository.findByModeloId(modelo.getId())).thenReturn(List.of(foto));

    final List<FotoGaleriaModelo> resultado = useCase.execute(modelo.getId());

    assertEquals(1, resultado.size());
  }

  @Test
  void deveFalharQuandoModeloNaoExiste() {
    final UUID modeloId = UUID.randomUUID();
    when(modeloRepository.findById(modeloId)).thenReturn(Optional.empty());

    assertThrows(RecursoNaoEncontradoException.class, () -> useCase.execute(modeloId));
  }
}
