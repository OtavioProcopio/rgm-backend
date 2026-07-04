package com.rgm.api.core.application.usecases.maquina;

import com.rgm.api.core.domain.model.aggregates.Maquina;
import com.rgm.api.core.domain.ports.repositories.MaquinaRepository;
import java.util.List;

/** Listar todas as maquinas do catalogo (qualquer usuario autenticado). */
public final class ListarMaquinasUseCase {

  private final MaquinaRepository maquinaRepository;

  public ListarMaquinasUseCase(final MaquinaRepository maquinaRepository) {
    this.maquinaRepository = maquinaRepository;
  }

  public List<Maquina> execute() {
    return maquinaRepository.findAll();
  }
}
