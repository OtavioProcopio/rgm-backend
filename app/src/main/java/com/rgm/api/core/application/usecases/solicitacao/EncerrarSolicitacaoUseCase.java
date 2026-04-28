package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.events.SolicitacaoFinalizadaEvent;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.DomainEventPublisher;
import java.time.Instant;
import java.util.UUID;

/** UC-07: Encerrar solicitacao (EM_VALIDACAO -> CONCLUIDA ou CANCELADA). */
public final class EncerrarSolicitacaoUseCase {

  private final SolicitacaoRepository solicitacaoRepository;
  private final UsuarioRepository usuarioRepository;
  private final AtividadeSolicitacaoRepository atividadeRepository;
  private final DomainEventPublisher eventPublisher;

  public EncerrarSolicitacaoUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final UsuarioRepository usuarioRepository,
      final AtividadeSolicitacaoRepository atividadeRepository,
      final DomainEventPublisher eventPublisher) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.usuarioRepository = usuarioRepository;
    this.atividadeRepository = atividadeRepository;
    this.eventPublisher = eventPublisher;
  }

  public record Input(
      UUID solicitacaoId, boolean concluir, String comentarioFinal, UUID gestorId) {}

  public Solicitacao execute(final Input input) {
    final Instant agora = Instant.now();

    final Usuario gestor =
        usuarioRepository
            .findById(input.gestorId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Gestor nao encontrado"));

    if (!gestor.getPerfil().podeEncerrar()) {
      throw new NaoAutorizadoException("Perfil sem permissao para encerrar solicitacoes");
    }

    final Solicitacao solicitacao =
        solicitacaoRepository
            .findById(input.solicitacaoId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao nao encontrada"));

    final Solicitacao encerrada;
    final StatusSolicitacao novoStatus;

    if (input.concluir()) {
      encerrada = solicitacao.concluir(input.comentarioFinal(), agora);
      novoStatus = StatusSolicitacao.CONCLUIDA;
    } else {
      encerrada = solicitacao.cancelar(input.comentarioFinal(), agora);
      novoStatus = StatusSolicitacao.CANCELADA;
    }

    final Solicitacao salva = solicitacaoRepository.save(encerrada);

    atividadeRepository.save(
        AtividadeSolicitacao.mudancaStatus(
            salva.getId(), solicitacao.getStatus(), novoStatus, input.gestorId(), agora));

    eventPublisher.publish(
        new SolicitacaoFinalizadaEvent(salva.getId(), salva.getModeloId(), novoStatus, agora));

    return salva;
  }
}
