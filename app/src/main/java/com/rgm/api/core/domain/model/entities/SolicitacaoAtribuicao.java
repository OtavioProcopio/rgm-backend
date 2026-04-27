package com.rgm.api.core.domain.model.entities;

import static com.rgm.api.core.domain.validation.DomainValidations.requireNonNull;

import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import java.time.Instant;
import java.util.UUID;

/** Atribuicao de uma solicitacao a um usuario (permite multiplas atribuicoes e historico). */
public final class SolicitacaoAtribuicao {

  private final UUID id;
  private final UUID solicitacaoId;
  private final UUID usuarioId;
  private final UUID atribuidoPorUsuarioId;
  private final Instant atribuidoEm;
  private final Instant removidoEm;

  public SolicitacaoAtribuicao(
      final UUID id,
      final UUID solicitacaoId,
      final UUID usuarioId,
      final UUID atribuidoPorUsuarioId,
      final Instant atribuidoEm,
      final Instant removidoEm) {
    this.id = requireNonNull(id, "id");
    this.solicitacaoId = requireNonNull(solicitacaoId, "solicitacaoId");
    this.usuarioId = requireNonNull(usuarioId, "usuarioId");
    this.atribuidoPorUsuarioId = requireNonNull(atribuidoPorUsuarioId, "atribuidoPorUsuarioId");
    this.atribuidoEm = requireNonNull(atribuidoEm, "atribuidoEm");
    this.removidoEm = removidoEm;
  }

  /** Cria uma nova atribuicao ativa. */
  public static SolicitacaoAtribuicao criar(
      final UUID solicitacaoId,
      final UUID usuarioId,
      final UUID atribuidoPorUsuarioId,
      final Instant agora) {
    return new SolicitacaoAtribuicao(
        UUID.randomUUID(), solicitacaoId, usuarioId, atribuidoPorUsuarioId, agora, null);
  }

  /** Verifica se a atribuicao esta ativa (sem data de remocao). */
  public boolean isAtiva() {
    return removidoEm == null;
  }

  /** Remove a atribuicao (marca com data de remocao). */
  public SolicitacaoAtribuicao remover(final Instant agora) {
    if (!isAtiva()) {
      throw new BusinessRuleException("Atribuicao ja foi removida");
    }
    return new SolicitacaoAtribuicao(
        id, solicitacaoId, usuarioId, atribuidoPorUsuarioId, atribuidoEm, agora);
  }

  public UUID getId() {
    return id;
  }

  public UUID getSolicitacaoId() {
    return solicitacaoId;
  }

  public UUID getUsuarioId() {
    return usuarioId;
  }

  public UUID getAtribuidoPorUsuarioId() {
    return atribuidoPorUsuarioId;
  }

  public Instant getAtribuidoEm() {
    return atribuidoEm;
  }

  public Instant getRemovidoEm() {
    return removidoEm;
  }
}
