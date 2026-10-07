package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.validation.AcessoSolicitacao;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ListarAtividadesUseCase {

  private final SolicitacaoRepository solicitacaoRepository;
  private final AtividadeSolicitacaoRepository atividadeRepository;
  private final UsuarioRepository usuarioRepository;
  private final SolicitacaoAtribuicaoRepository atribuicaoRepository;

  public ListarAtividadesUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final AtividadeSolicitacaoRepository atividadeRepository,
      final UsuarioRepository usuarioRepository,
      final SolicitacaoAtribuicaoRepository atribuicaoRepository) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.atividadeRepository = atividadeRepository;
    this.usuarioRepository = usuarioRepository;
    this.atribuicaoRepository = atribuicaoRepository;
  }

  public record Input(UUID solicitacaoId, UUID usuarioId) {}

  public record AtividadeComAutor(AtividadeSolicitacao atividade, String autorNome) {}

  public List<AtividadeComAutor> execute(final Input input) {
    final UUID solicitacaoId = input.solicitacaoId();
    final Solicitacao solicitacao =
        solicitacaoRepository
            .findById(solicitacaoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao nao encontrada"));

    final Usuario usuario =
        usuarioRepository
            .findById(input.usuarioId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));

    AcessoSolicitacao.validarLeitura(
        usuario,
        solicitacao,
        () ->
            atribuicaoRepository.existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(
                solicitacaoId, usuario.getId()));

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
