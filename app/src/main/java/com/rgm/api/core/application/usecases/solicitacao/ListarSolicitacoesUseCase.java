package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import java.time.Instant;
import java.util.UUID;

/** Listar solicitacoes com paginacao e filtros opcionais. */
public final class ListarSolicitacoesUseCase {

  private final SolicitacaoRepository solicitacaoRepository;

  public ListarSolicitacoesUseCase(final SolicitacaoRepository solicitacaoRepository) {
    this.solicitacaoRepository = solicitacaoRepository;
  }

  public record Input(
      StatusSolicitacao status,
      UUID modeloId,
      TipoSolicitacao tipo,
      PrioridadeSolicitacao prioridade,
      Instant criadaEmInicio,
      Instant criadaEmFim,
      UUID abertaPorUsuarioId,
      UUID responsavelId,
      int page,
      int size) {}

  public PageResult<Solicitacao> execute(final Input input) {
    if (input.status() != null
        || input.modeloId() != null
        || input.tipo() != null
        || input.prioridade() != null
        || input.criadaEmInicio() != null
        || input.criadaEmFim() != null
        || input.abertaPorUsuarioId() != null
        || input.responsavelId() != null) {
      return solicitacaoRepository.findByFilters(
          input.status(),
          input.modeloId(),
          input.tipo(),
          input.prioridade(),
          input.criadaEmInicio(),
          input.criadaEmFim(),
          input.abertaPorUsuarioId(),
          input.responsavelId(),
          input.page(),
          input.size());
    }
    return solicitacaoRepository.findAll(input.page(), input.size());
  }
}
