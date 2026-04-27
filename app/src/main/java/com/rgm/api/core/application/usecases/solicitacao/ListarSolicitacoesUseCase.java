package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Listar solicitacoes com paginacao e filtro opcional por status. */
public final class ListarSolicitacoesUseCase {
  private static final Logger log = LoggerFactory.getLogger(ListarSolicitacoesUseCase.class);

  private final SolicitacaoRepository solicitacaoRepository;

  public ListarSolicitacoesUseCase(final SolicitacaoRepository solicitacaoRepository) {
    this.solicitacaoRepository = solicitacaoRepository;
  }

  public record Input(StatusSolicitacao status, int page, int size) {}

  public PageResult<Solicitacao> execute(final Input input) {
    log.info("ListarSolicitacoesUseCase.execute iniciado");
    if (input.status() != null) {
      return solicitacaoRepository.findByStatus(input.status(), input.page(), input.size());
    }
    return solicitacaoRepository.findAll(input.page(), input.size());
  }
}
