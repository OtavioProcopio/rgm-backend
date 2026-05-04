package com.rgm.api.core.application.usecases.admin;

import com.rgm.api.core.domain.model.aggregates.Maquina;
import com.rgm.api.core.domain.ports.repositories.MaquinaRepository;
import com.rgm.api.core.domain.ports.repositories.PageResult;

/** Listar maquinas com paginacao. */
public final class ListarMaquinasUseCase {

  private final MaquinaRepository maquinaRepository;

  public ListarMaquinasUseCase(final MaquinaRepository maquinaRepository) {
    this.maquinaRepository = maquinaRepository;
  }

  public PageResult<Maquina> execute(final int page, final int size) {
    return maquinaRepository.findAll(page, size);
  }
}
