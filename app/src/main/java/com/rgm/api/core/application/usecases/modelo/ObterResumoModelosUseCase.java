package com.rgm.api.core.application.usecases.modelo;

import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.ResumoModelos;

/** Contagens do cadastro de modelos, sem trazer a lista. */
public final class ObterResumoModelosUseCase {

  private final ModeloRepository modeloRepository;

  public ObterResumoModelosUseCase(final ModeloRepository modeloRepository) {
    this.modeloRepository = modeloRepository;
  }

  public ResumoModelos execute() {
    return modeloRepository.resumir();
  }
}
