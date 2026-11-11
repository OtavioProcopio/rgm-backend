package com.rgm.api.core.application.usecases.modelo;

import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.ports.repositories.FiltrosDeModelos;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.PosicaoDoModelo;
import java.util.UUID;

/** Em que posicao do filtro, em ordem de codigo, um modelo esta: base do link direto. */
public final class PosicaoDoModeloUseCase {

  private final ModeloRepository modeloRepository;

  public PosicaoDoModeloUseCase(final ModeloRepository modeloRepository) {
    this.modeloRepository = modeloRepository;
  }

  public PosicaoDoModelo execute(final UUID id, final FiltrosDeModelos filtros) {
    if (modeloRepository.findById(id).isEmpty()) {
      throw new RecursoNaoEncontradoException("Modelo nao encontrado");
    }
    return modeloRepository.posicaoDe(id, filtros);
  }
}
