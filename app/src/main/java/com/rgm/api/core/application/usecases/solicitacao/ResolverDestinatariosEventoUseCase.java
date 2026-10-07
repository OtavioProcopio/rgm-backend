package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.validation.AcessoSolicitacao;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Decide quem, entre os usuarios conectados ao tempo real, recebe o evento de uma solicitacao: quem
 * tem acesso a ela agora e quem tinha acesso antes da mudanca que gerou o evento.
 */
public final class ResolverDestinatariosEventoUseCase {

  private final SolicitacaoRepository solicitacaoRepository;
  private final SolicitacaoAtribuicaoRepository atribuicaoRepository;
  private final UsuarioRepository usuarioRepository;

  public ResolverDestinatariosEventoUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final SolicitacaoAtribuicaoRepository atribuicaoRepository,
      final UsuarioRepository usuarioRepository) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.atribuicaoRepository = atribuicaoRepository;
    this.usuarioRepository = usuarioRepository;
  }

  /** {@code comAcessoAnterior}: responsaveis removidos pela mudanca que gerou o evento. */
  public record Input(UUID solicitacaoId, Set<UUID> conectados, Set<UUID> comAcessoAnterior) {}

  public Set<UUID> execute(final Input input) {
    if (input.conectados().isEmpty()) {
      return Set.of();
    }

    final Optional<Solicitacao> encontrada = solicitacaoRepository.findById(input.solicitacaoId());
    if (encontrada.isEmpty()) {
      return Set.of();
    }
    final Solicitacao solicitacao = encontrada.get();

    final Set<UUID> responsaveis =
        atribuicaoRepository.findBySolicitacaoId(input.solicitacaoId()).stream()
            .filter(a -> a.getRemovidoEm() == null)
            .map(a -> a.getUsuarioId())
            .collect(Collectors.toSet());

    return usuarioRepository.findAllByIdIn(List.copyOf(input.conectados())).stream()
        .filter(
            usuario ->
                AcessoSolicitacao.podeVer(
                    usuario,
                    solicitacao,
                    () ->
                        responsaveis.contains(usuario.getId())
                            || input.comAcessoAnterior().contains(usuario.getId())))
        .map(Usuario::getId)
        .collect(Collectors.toUnmodifiableSet());
  }
}
