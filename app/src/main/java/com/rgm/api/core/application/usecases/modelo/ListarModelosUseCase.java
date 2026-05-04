package com.rgm.api.core.application.usecases.modelo;

import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.PageResult;

/** Listar modelos com paginacao. */
public final class ListarModelosUseCase {

  private final ModeloRepository modeloRepository;

  public ListarModelosUseCase(final ModeloRepository modeloRepository) {
    this.modeloRepository = modeloRepository;
  }

  public PageResult<Modelo> execute(final int page, final int size) {
    return modeloRepository.findAll(page, size);
  }
}
