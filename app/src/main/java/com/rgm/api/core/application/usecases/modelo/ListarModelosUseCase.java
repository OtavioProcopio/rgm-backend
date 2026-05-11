package com.rgm.api.core.application.usecases.modelo;

import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.PageResult;

/** Listar modelos com paginacao e filtros opcionais. */
public final class ListarModelosUseCase {

  private final ModeloRepository modeloRepository;

  public ListarModelosUseCase(final ModeloRepository modeloRepository) {
    this.modeloRepository = modeloRepository;
  }

  public record Input(Boolean ativo, String codigo, int page, int size) {}

  public PageResult<Modelo> execute(final Input input) {
    if (input.ativo() != null || input.codigo() != null) {
      return modeloRepository.findByFilters(
          input.ativo(), input.codigo(), input.page(), input.size());
    }
    return modeloRepository.findAll(input.page(), input.size());
  }
}
