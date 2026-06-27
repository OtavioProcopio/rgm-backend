package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ListarAtividadesUseCase {

  private final SolicitacaoRepository solicitacaoRepository;
  private final AtividadeSolicitacaoRepository atividadeRepository;
  private final UsuarioRepository usuarioRepository;

  public ListarAtividadesUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final AtividadeSolicitacaoRepository atividadeRepository,
      final UsuarioRepository usuarioRepository) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.atividadeRepository = atividadeRepository;
    this.usuarioRepository = usuarioRepository;
  }

  public record AtividadeComAutor(AtividadeSolicitacao atividade, String autorNome) {}

  public List<AtividadeComAutor> execute(final UUID solicitacaoId) {
    solicitacaoRepository
        .findById(solicitacaoId)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao nao encontrada"));

    final List<AtividadeSolicitacao> atividades =
        atividadeRepository.findBySolicitacaoId(solicitacaoId);

    final List<UUID> autorIds =
        atividades.stream().map(a -> a.getAutorUsuarioId()).distinct().toList();

    final Map<UUID, String> nomesPorId =
        usuarioRepository.findAllByIdIn(autorIds).stream()
            .collect(Collectors.toMap(u -> u.getId(), u -> u.getNome()));

    return atividades.stream()
        .map(
            a ->
                new AtividadeComAutor(a, nomesPorId.getOrDefault(a.getAutorUsuarioId(), "Usuário")))
        .toList();
  }
}
