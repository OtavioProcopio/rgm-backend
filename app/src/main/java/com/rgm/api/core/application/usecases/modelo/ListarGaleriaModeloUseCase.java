package com.rgm.api.core.application.usecases.modelo;

import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.FotoGaleriaModelo;
import com.rgm.api.core.domain.ports.repositories.FotoGaleriaModeloRepository;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import java.util.List;
import java.util.UUID;

/** Lista as fotos da galeria de um Modelo (qualquer usuario autenticado). */
public final class ListarGaleriaModeloUseCase {

  private final ModeloRepository modeloRepository;
  private final FotoGaleriaModeloRepository fotoGaleriaModeloRepository;

  public ListarGaleriaModeloUseCase(
      final ModeloRepository modeloRepository,
      final FotoGaleriaModeloRepository fotoGaleriaModeloRepository) {
    this.modeloRepository = modeloRepository;
    this.fotoGaleriaModeloRepository = fotoGaleriaModeloRepository;
  }

  public List<FotoGaleriaModelo> execute(final UUID modeloId) {
    modeloRepository
        .findById(modeloId)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Modelo nao encontrado"));

    return fotoGaleriaModeloRepository.findByModeloId(modeloId);
  }
}
