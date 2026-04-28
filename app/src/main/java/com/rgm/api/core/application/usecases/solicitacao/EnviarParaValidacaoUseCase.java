package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** UC-04/UC-05: Enviar para validacao (EM_ANDAMENTO -> EM_VALIDACAO) com autorizacao central. */
public final class EnviarParaValidacaoUseCase {
  private static final Logger log = LoggerFactory.getLogger(EnviarParaValidacaoUseCase.class);

  private final SolicitacaoRepository solicitacaoRepository;
  private final UsuarioRepository usuarioRepository;
  private final SolicitacaoAtribuicaoRepository atribuicaoRepository;
  private final AtividadeSolicitacaoRepository atividadeRepository;

  public EnviarParaValidacaoUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final UsuarioRepository usuarioRepository,
      final SolicitacaoAtribuicaoRepository atribuicaoRepository,
      final AtividadeSolicitacaoRepository atividadeRepository) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.usuarioRepository = usuarioRepository;
    this.atribuicaoRepository = atribuicaoRepository;
    this.atividadeRepository = atividadeRepository;
  }

  public record Input(UUID solicitacaoId, UUID usuarioId) {}

  public Solicitacao execute(final Input input) {
    log.info("EnviarParaValidacaoUseCase.execute iniciado");
    final Instant agora = Instant.now();

    final Usuario usuario =
        usuarioRepository
            .findById(input.usuarioId())
            .orElseThrow(() -> new ValidationException("Usuario nao encontrado"));

    final Solicitacao solicitacao =
        solicitacaoRepository
            .findById(input.solicitacaoId())
            .orElseThrow(() -> new ValidationException("Solicitacao nao encontrada"));

    final boolean estaAtribuido =
        atribuicaoRepository.existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(
            solicitacao.getId(), usuario.getId());

    Solicitacao.validarAutorizacaoMover(
        usuario.getPerfil(),
        solicitacao.getStatus(),
        StatusSolicitacao.EM_VALIDACAO,
        estaAtribuido);

    final Solicitacao atualizada = solicitacao.enviarParaValidacao(agora);
    final Solicitacao salva = solicitacaoRepository.save(atualizada);

    atividadeRepository.save(
        AtividadeSolicitacao.mudancaStatus(
            salva.getId(),
            StatusSolicitacao.EM_ANDAMENTO,
            StatusSolicitacao.EM_VALIDACAO,
            input.usuarioId(),
            agora));

    return salva;
  }
}
