package com.rgm.api.core.application.usecases.modelo;

import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.ResumoSolicitacoesModelo;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import java.util.UUID;

/** Resumo das solicitacoes de um modelo, sem trazer a lista. */
public final class ObterResumoSolicitacoesModeloUseCase {

  private final ModeloRepository modeloRepository;
  private final SolicitacaoRepository solicitacaoRepository;

  public ObterResumoSolicitacoesModeloUseCase(
      final ModeloRepository modeloRepository, final SolicitacaoRepository solicitacaoRepository) {
    this.modeloRepository = modeloRepository;
    this.solicitacaoRepository = solicitacaoRepository;
  }

  public ResumoSolicitacoesModelo execute(final UUID modeloId) {
    modeloRepository
        .findById(modeloId)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Modelo nao encontrado"));
    return solicitacaoRepository.resumirPorModelo(modeloId);
  }
}
