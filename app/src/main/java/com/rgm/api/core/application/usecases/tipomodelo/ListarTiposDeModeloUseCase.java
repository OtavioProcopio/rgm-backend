package com.rgm.api.core.application.usecases.tipomodelo;

import com.rgm.api.core.domain.model.aggregates.TipoDeModelo;
import com.rgm.api.core.domain.ports.repositories.TipoDeModeloRepository;
import java.util.List;

/** Listar todas as tiposDeModelo do catalogo (qualquer usuario autenticado). */
public final class ListarTiposDeModeloUseCase {

  private final TipoDeModeloRepository tipoDeModeloRepository;

  public ListarTiposDeModeloUseCase(final TipoDeModeloRepository tipoDeModeloRepository) {
    this.tipoDeModeloRepository = tipoDeModeloRepository;
  }

  public List<TipoDeModelo> execute() {
    return tipoDeModeloRepository.findAll();
  }
}
