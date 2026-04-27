package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** UC-12 (parcial): Registrar comentario em solicitacao (Gestor como procurador do externo). */
public final class RegistrarComentarioUseCase {
  private static final Logger log = LoggerFactory.getLogger(RegistrarComentarioUseCase.class);

  private final SolicitacaoRepository solicitacaoRepository;
  private final AtividadeSolicitacaoRepository atividadeRepository;

  public RegistrarComentarioUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final AtividadeSolicitacaoRepository atividadeRepository) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.atividadeRepository = atividadeRepository;
  }

  public record Input(UUID solicitacaoId, String comentario, UUID autorId) {}

  public AtividadeSolicitacao execute(final Input input) {
    log.info("RegistrarComentarioUseCase.execute iniciado");
    final Instant agora = Instant.now();

    solicitacaoRepository
        .findById(input.solicitacaoId())
        .orElseThrow(() -> new ValidationException("Solicitacao nao encontrada"));

    if (input.comentario() == null || input.comentario().isBlank()) {
      throw new ValidationException("Comentario e obrigatorio");
    }

    final AtividadeSolicitacao atividade =
        AtividadeSolicitacao.comentario(
            input.solicitacaoId(), input.comentario(), input.autorId(), agora);

    return atividadeRepository.save(atividade);
  }
}
