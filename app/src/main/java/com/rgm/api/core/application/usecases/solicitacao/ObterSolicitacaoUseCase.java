package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ObterSolicitacaoUseCase {

  private final SolicitacaoRepository solicitacaoRepository;
  private final SolicitacaoAtribuicaoRepository atribuicaoRepository;

  public ObterSolicitacaoUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final SolicitacaoAtribuicaoRepository atribuicaoRepository) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.atribuicaoRepository = atribuicaoRepository;
  }

  public record Output(Solicitacao solicitacao, List<UUID> responsavelIds) {}

  public Output execute(final UUID id) {
    final Solicitacao solicitacao =
        solicitacaoRepository
            .findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao nao encontrada"));

    final List<UUID> responsavelIds =
        atribuicaoRepository.findBySolicitacaoId(id).stream()
            .filter(a -> a.getRemovidoEm() == null)
            .map(a -> a.getUsuarioId())
            .toList();

    return new Output(solicitacao, responsavelIds);
  }

  public Map<UUID, List<UUID>> listarResponsaveisBatch(final List<UUID> solicitacaoIds) {
    if (solicitacaoIds.isEmpty()) {
      return Map.of();
    }
    return atribuicaoRepository.findBySolicitacaoIdIn(solicitacaoIds).stream()
        .filter(a -> a.getRemovidoEm() == null)
        .collect(
            Collectors.groupingBy(
                a -> a.getSolicitacaoId(),
                Collectors.mapping(a -> a.getUsuarioId(), Collectors.toList())));
  }
}
