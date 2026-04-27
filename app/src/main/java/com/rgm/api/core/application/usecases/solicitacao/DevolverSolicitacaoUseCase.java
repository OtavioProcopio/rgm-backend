package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.UUID;

/** UC-06: Devolver para correcao (EM_VALIDACAO -> EM_ANDAMENTO). */
public final class DevolverSolicitacaoUseCase {

  private final SolicitacaoRepository solicitacaoRepository;
  private final UsuarioRepository usuarioRepository;
  private final AtividadeSolicitacaoRepository atividadeRepository;

  public DevolverSolicitacaoUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final UsuarioRepository usuarioRepository,
      final AtividadeSolicitacaoRepository atividadeRepository) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.usuarioRepository = usuarioRepository;
    this.atividadeRepository = atividadeRepository;
  }

  public record Input(
      UUID solicitacaoId, String motivo, PrioridadeSolicitacao novaPrioridade, UUID gestorId) {}

  public Solicitacao execute(final Input input) {
    final Instant agora = Instant.now();

    if (input.motivo() == null || input.motivo().isBlank()) {
      throw new ValidationException("Motivo e obrigatorio para devolucao");
    }

    final Usuario gestor =
        usuarioRepository
            .findById(input.gestorId())
            .orElseThrow(() -> new ValidationException("Gestor nao encontrado"));

    if (!gestor.getPerfil().podeDevolver()) {
      throw new NaoAutorizadoException("Perfil sem permissao para devolver solicitacoes");
    }

    final Solicitacao solicitacao =
        solicitacaoRepository
            .findById(input.solicitacaoId())
            .orElseThrow(() -> new ValidationException("Solicitacao nao encontrada"));

    final Solicitacao devolvida = solicitacao.devolver(input.novaPrioridade(), agora);
    final Solicitacao salva = solicitacaoRepository.save(devolvida);

    atividadeRepository.save(
        AtividadeSolicitacao.mudancaStatus(
            salva.getId(),
            StatusSolicitacao.EM_VALIDACAO,
            StatusSolicitacao.EM_ANDAMENTO,
            input.gestorId(),
            agora));

    atividadeRepository.save(
        AtividadeSolicitacao.comentario(salva.getId(), input.motivo(), input.gestorId(), agora));

    return salva;
  }
}
