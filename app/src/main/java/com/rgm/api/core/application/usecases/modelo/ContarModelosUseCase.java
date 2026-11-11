package com.rgm.api.core.application.usecases.modelo;

import com.rgm.api.core.domain.ports.repositories.ContagensDeModelos;
import com.rgm.api.core.domain.ports.repositories.FiltrosDeModelos;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;

/** Quantos modelos cada botao de tipo e de maquina do catalogo traria. */
public final class ContarModelosUseCase {

  private final ModeloRepository modeloRepository;

  public ContarModelosUseCase(final ModeloRepository modeloRepository) {
    this.modeloRepository = modeloRepository;
  }

  public ContagensDeModelos execute(final FiltrosDeModelos filtros) {
    return modeloRepository.contar(filtros);
  }
}
