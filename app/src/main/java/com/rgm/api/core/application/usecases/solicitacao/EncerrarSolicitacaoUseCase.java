package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.application.usecases.modelo.GerenciarModelosUseCase;
import com.rgm.api.core.domain.events.SolicitacaoFinalizadaEvent;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.EventoModelo;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoEventoModelo;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.EventoModeloRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.DomainEventPublisher;
import java.time.Instant;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** UC-07: Encerrar solicitacao (EM_VALIDACAO -> CONCLUIDA ou CANCELADA). */
@Transactional
public class EncerrarSolicitacaoUseCase {

  private final SolicitacaoRepository solicitacaoRepository;
  private final UsuarioRepository usuarioRepository;
  private final AtividadeSolicitacaoRepository atividadeRepository;
  private final EventoModeloRepository eventoModeloRepository;
  private final DomainEventPublisher eventPublisher;
  private final GerenciarModelosUseCase gerenciarModelosUseCase;
  private final CancelarSolicitacaoUseCase cancelarUseCase;

  public EncerrarSolicitacaoUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final UsuarioRepository usuarioRepository,
      final AtividadeSolicitacaoRepository atividadeRepository,
      final EventoModeloRepository eventoModeloRepository,
      final DomainEventPublisher eventPublisher,
      final GerenciarModelosUseCase gerenciarModelosUseCase,
      final CancelarSolicitacaoUseCase cancelarUseCase) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.usuarioRepository = usuarioRepository;
    this.atividadeRepository = atividadeRepository;
    this.eventoModeloRepository = eventoModeloRepository;
    this.eventPublisher = eventPublisher;
    this.gerenciarModelosUseCase = gerenciarModelosUseCase;
    this.cancelarUseCase = cancelarUseCase;
  }

  public record Input(
      UUID solicitacaoId, boolean concluir, String comentarioFinal, UUID gestorId) {}

  public Solicitacao execute(final Input input) {
    final Instant agora = Instant.now();

    if (input.comentarioFinal() == null || input.comentarioFinal().isBlank()) {
      throw new ValidationException("Comentario final e obrigatorio para encerrar solicitacao");
    }

    final Usuario gestor =
        usuarioRepository
            .findById(input.gestorId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Gestor nao encontrado"));

    if (!gestor.getPerfil().podeEncerrar()) {
      throw new NaoAutorizadoException("Perfil sem permissao para encerrar solicitacoes");
    }

    // Cancelar deixa a mesma trilha (status e motivo) pelos dois endpoints.
    if (!input.concluir()) {
      return cancelarUseCase.execute(
          new CancelarSolicitacaoUseCase.Input(
              input.solicitacaoId(), input.comentarioFinal(), input.gestorId()));
    }

    final Solicitacao solicitacao =
        solicitacaoRepository
            .findById(input.solicitacaoId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao nao encontrada"));

    final boolean geradaPorCriacao = solicitacao.getTipo() == TipoSolicitacao.CRIACAO;
    final Solicitacao encerrada;
    if (geradaPorCriacao) {
      final Modelo modeloCriado =
          gerenciarModelosUseCase.criar(
              new GerenciarModelosUseCase.CriarInput(
                  solicitacao.getModeloCodigo(),
                  solicitacao.getDescricao(),
                  solicitacao.getModeloObservacoes(),
                  solicitacao.getModeloMaquina(),
                  null,
                  input.gestorId(),
                  solicitacao.getId()));
      encerrada = solicitacao.concluirCriacao(input.comentarioFinal(), modeloCriado.getId(), agora);
    } else {
      encerrada = solicitacao.concluir(input.comentarioFinal(), agora);
    }
    final StatusSolicitacao novoStatus = StatusSolicitacao.CONCLUIDA;

    final Solicitacao salva = solicitacaoRepository.save(encerrada);

    atividadeRepository.save(
        AtividadeSolicitacao.mudancaStatus(
            salva.getId(), solicitacao.getStatus(), novoStatus, input.gestorId(), agora));

    // Para CRIACAO, o evento CADASTRO ja foi criado dentro de gerenciarModelosUseCase.criar().
    if (novoStatus == StatusSolicitacao.CONCLUIDA
        && !geradaPorCriacao
        && salva.getModeloId() != null) {
      eventoModeloRepository.save(
          EventoModelo.criar(
              salva.getModeloId(),
              mapearTipoEvento(salva.getTipo()),
              salva.getTitulo(),
              input.comentarioFinal(),
              null,
              input.gestorId(),
              salva.getId(),
              agora));
    }

    eventPublisher.publish(
        new SolicitacaoFinalizadaEvent(salva.getId(), salva.getModeloId(), novoStatus, agora));

    return salva;
  }

  private static TipoEventoModelo mapearTipoEvento(final TipoSolicitacao tipo) {
    return switch (tipo) {
      case REPARO -> TipoEventoModelo.REPARO;
      case INSPECAO -> TipoEventoModelo.INSPECAO;
      case REENGENHARIA -> TipoEventoModelo.MODIFICACAO;
      case CRIACAO -> throw new IllegalStateException(
          "CRIACAO nao usa mapearTipoEvento — o evento e criado dentro de"
              + " GerenciarModelosUseCase.criar()");
    };
  }
}
