package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.model.entities.SolicitacaoAtribuicao;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** UC-03: Triar e atribuir (A_FAZER -> EM_ANDAMENTO). */
public final class TriarSolicitacaoUseCase {

  private final SolicitacaoRepository solicitacaoRepository;
  private final UsuarioRepository usuarioRepository;
  private final SolicitacaoAtribuicaoRepository atribuicaoRepository;
  private final AtividadeSolicitacaoRepository atividadeRepository;

  public TriarSolicitacaoUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final UsuarioRepository usuarioRepository,
      final SolicitacaoAtribuicaoRepository atribuicaoRepository,
      final AtividadeSolicitacaoRepository atividadeRepository) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.usuarioRepository = usuarioRepository;
    this.atribuicaoRepository = atribuicaoRepository;
    this.atividadeRepository = atividadeRepository;
  }

  public record Input(
      UUID solicitacaoId,
      PrioridadeSolicitacao prioridade,
      List<UUID> responsavelIds,
      UUID gestorId) {}

  public Solicitacao execute(final Input input) {
    final Instant agora = Instant.now();

    final Usuario gestor =
        usuarioRepository
            .findById(input.gestorId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Gestor nao encontrado"));

    if (!gestor.getPerfil().podeTriar()) {
      throw new NaoAutorizadoException("Perfil sem permissao para triar solicitacoes");
    }

    final Solicitacao solicitacao =
        solicitacaoRepository
            .findById(input.solicitacaoId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao nao encontrada"));

    if (input.responsavelIds() == null || input.responsavelIds().isEmpty()) {
      throw new BusinessRuleException("Deve ter pelo menos 1 responsavel");
    }

    final List<Usuario> responsaveis = usuarioRepository.findAllByIdIn(input.responsavelIds());

    if (responsaveis.size() != input.responsavelIds().size()) {
      throw new RecursoNaoEncontradoException("Um ou mais responsaveis nao encontrados");
    }

    for (final Usuario resp : responsaveis) {
      if (!resp.isAtivo()) {
        throw new BusinessRuleException("Responsavel inativo: " + resp.getNome());
      }
      Solicitacao.validarPerfilAtribuivel(resp.getPerfil());
    }

    final Solicitacao triada = solicitacao.triar(input.prioridade(), agora);
    final Solicitacao salva = solicitacaoRepository.save(triada);

    for (final Usuario resp : responsaveis) {
      atribuicaoRepository.save(
          SolicitacaoAtribuicao.criar(salva.getId(), resp.getId(), input.gestorId(), agora));
    }

    atividadeRepository.save(
        AtividadeSolicitacao.atribuicao(salva.getId(), input.gestorId(), null, agora));

    atividadeRepository.save(
        AtividadeSolicitacao.mudancaStatus(
            salva.getId(),
            StatusSolicitacao.A_FAZER,
            StatusSolicitacao.EM_ANDAMENTO,
            input.gestorId(),
            agora));

    return salva;
  }
}
