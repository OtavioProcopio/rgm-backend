package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import java.time.Instant;
import java.util.UUID;

/** UC-12 (parcial): Registrar comentario em solicitacao (Gestor como procurador do externo). */
public final class RegistrarComentarioUseCase {

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
    final Instant agora = Instant.now();

    solicitacaoRepository
        .findById(input.solicitacaoId())
        .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao nao encontrada"));

    if (input.comentario() == null || input.comentario().isBlank()) {
      throw new ValidationException("Comentario e obrigatorio");
    }

    final AtividadeSolicitacao atividade =
        AtividadeSolicitacao.comentario(
            input.solicitacaoId(), input.comentario(), input.autorId(), agora);

    return atividadeRepository.save(atividade);
  }
}
