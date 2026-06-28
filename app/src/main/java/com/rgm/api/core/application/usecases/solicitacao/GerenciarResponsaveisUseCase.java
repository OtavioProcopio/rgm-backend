package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.model.entities.SolicitacaoAtribuicao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Caso de uso para gerenciar responsaveis de uma solicitacao (adicionar/remover). */
public final class GerenciarResponsaveisUseCase {

  private final SolicitacaoRepository solicitacaoRepository;
  private final UsuarioRepository usuarioRepository;
  private final SolicitacaoAtribuicaoRepository atribuicaoRepository;
  private final AtividadeSolicitacaoRepository atividadeRepository;

  public GerenciarResponsaveisUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final UsuarioRepository usuarioRepository,
      final SolicitacaoAtribuicaoRepository atribuicaoRepository,
      final AtividadeSolicitacaoRepository atividadeRepository) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.usuarioRepository = usuarioRepository;
    this.atribuicaoRepository = atribuicaoRepository;
    this.atividadeRepository = atividadeRepository;
  }

  public record Input(UUID solicitacaoId, List<UUID> responsavelIds, UUID gestorId) {}

  public Solicitacao execute(final Input input) {
    final Instant agora = Instant.now();

    final Usuario gestor =
        usuarioRepository
            .findById(input.gestorId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Gestor nao encontrado"));

    if (!gestor.getPerfil().podeTriar()) {
      throw new NaoAutorizadoException("Perfil sem permissao para gerenciar responsaveis");
    }

    final Solicitacao solicitacao =
        solicitacaoRepository
            .findById(input.solicitacaoId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao nao encontrada"));

    if (solicitacao.getStatus().isTerminal()) {
      throw new BusinessRuleException(
          "Nao e possivel gerenciar responsaveis de uma solicitacao encerrada");
    }

    if (input.responsavelIds() == null || input.responsavelIds().isEmpty()) {
      throw new BusinessRuleException("Deve ter pelo menos 1 responsavel");
    }

    final List<Usuario> novosResponsaveis = usuarioRepository.findAllByIdIn(input.responsavelIds());

    if (novosResponsaveis.size() != input.responsavelIds().size()) {
      throw new RecursoNaoEncontradoException("Um ou mais responsaveis nao encontrados");
    }

    for (final Usuario resp : novosResponsaveis) {
      if (!resp.isAtivo()) {
        throw new BusinessRuleException("Responsavel inativo: " + resp.getNome());
      }
      Solicitacao.validarPerfilAtribuivel(resp.getPerfil());
    }

    final List<SolicitacaoAtribuicao> atribuicoesAtuais =
        atribuicaoRepository.findBySolicitacaoId(input.solicitacaoId()).stream()
            .filter(a -> a.getRemovidoEm() == null)
            .toList();

    for (final var atr : atribuicoesAtuais) {
      if (!input.responsavelIds().contains(atr.getUsuarioId())) {
        final var removido = atr.remover(agora);
        atribuicaoRepository.save(removido);

        final String nomeResp =
            usuarioRepository.findById(atr.getUsuarioId()).map(Usuario::getNome).orElse("Usuario");

        atividadeRepository.save(
            AtividadeSolicitacao.atribuicao(
                solicitacao.getId(), input.gestorId(), "Responsavel removido: " + nomeResp, agora));
      }
    }

    for (final var resp : novosResponsaveis) {
      final boolean jaAtribuido =
          atribuicoesAtuais.stream().anyMatch(a -> a.getUsuarioId().equals(resp.getId()));
      if (!jaAtribuido) {
        atribuicaoRepository.save(
            SolicitacaoAtribuicao.criar(
                solicitacao.getId(), resp.getId(), input.gestorId(), agora));

        atividadeRepository.save(
            AtividadeSolicitacao.atribuicao(
                solicitacao.getId(),
                input.gestorId(),
                "Responsavel adicionado: " + resp.getNome(),
                agora));
      }
    }

    return solicitacao;
  }
}
