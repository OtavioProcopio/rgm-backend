package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.events.SolicitacaoFinalizadaEvent;
import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.DomainEventPublisher;
import java.time.Instant;
import java.util.UUID;

/** UC-07: Cancelar solicitacao (A_FAZER/EM_ANDAMENTO/EM_VALIDACAO -> CANCELADA). */
public final class CancelarSolicitacaoUseCase {

  private final SolicitacaoRepository solicitacaoRepository;
  private final UsuarioRepository usuarioRepository;
  private final AtividadeSolicitacaoRepository atividadeRepository;
  private final SolicitacaoAtribuicaoRepository atribuicaoRepository;
  private final DomainEventPublisher eventPublisher;

  public CancelarSolicitacaoUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final UsuarioRepository usuarioRepository,
      final AtividadeSolicitacaoRepository atividadeRepository,
      final SolicitacaoAtribuicaoRepository atribuicaoRepository,
      final DomainEventPublisher eventPublisher) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.usuarioRepository = usuarioRepository;
    this.atividadeRepository = atividadeRepository;
    this.atribuicaoRepository = atribuicaoRepository;
    this.eventPublisher = eventPublisher;
  }

  public record Input(UUID solicitacaoId, String motivo, UUID usuarioId) {}

  public Solicitacao execute(final Input input) {
    final Instant agora = Instant.now();

    if (input.motivo() == null || input.motivo().isBlank()) {
      throw new ValidationException("Motivo e obrigatorio para cancelar solicitacao");
    }

    final Usuario usuario =
        usuarioRepository
            .findById(input.usuarioId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));

    final Solicitacao solicitacao =
        solicitacaoRepository
            .findById(input.solicitacaoId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao nao encontrada"));

    validarPermissaoCancelar(usuario, solicitacao);

    final StatusSolicitacao statusAnterior = solicitacao.getStatus();
    final Solicitacao cancelada = solicitacao.cancelar(input.motivo(), agora);
    final Solicitacao salva = solicitacaoRepository.save(cancelada);

    atividadeRepository.save(
        AtividadeSolicitacao.mudancaStatus(
            salva.getId(), statusAnterior, StatusSolicitacao.CANCELADA, input.usuarioId(), agora));

    atividadeRepository.save(
        AtividadeSolicitacao.comentario(salva.getId(), input.motivo(), input.usuarioId(), agora));

    eventPublisher.publish(
        new SolicitacaoFinalizadaEvent(
            salva.getId(), salva.getModeloId(), StatusSolicitacao.CANCELADA, agora));

    return salva;
  }

  private void validarPermissaoCancelar(final Usuario usuario, final Solicitacao solicitacao) {
    if (usuario.getPerfil().podeEncerrar()) {
      return;
    }

    if (usuario.getPerfil().name().equals("OPERADOR")) {
      if (solicitacao.getStatus() != StatusSolicitacao.A_FAZER) {
        throw new NaoAutorizadoException(
            "Operador so pode cancelar solicitacoes em A_FAZER que ele mesmo abriu");
      }
      if (!solicitacao.getAbertaPorUsuarioId().equals(usuario.getId())) {
        throw new NaoAutorizadoException(
            "Operador so pode cancelar solicitacoes que ele mesmo abriu");
      }
      final boolean temResponsavel =
          atribuicaoRepository.findBySolicitacaoId(solicitacao.getId()).stream()
              .anyMatch(a -> a.getRemovidoEm() == null);
      if (temResponsavel) {
        throw new BusinessRuleException(
            "Nao e possivel cancelar solicitacao que ja possui responsavel atribuido."
                + " Solicite ao gestor.");
      }
      return;
    }

    throw new NaoAutorizadoException("Perfil sem permissao para cancelar solicitacoes");
  }
}
