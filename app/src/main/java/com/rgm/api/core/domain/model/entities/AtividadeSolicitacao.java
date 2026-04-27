package com.rgm.api.core.domain.model.entities;

import static com.rgm.api.core.domain.validation.DomainValidations.optionalTrimToNull;
import static com.rgm.api.core.domain.validation.DomainValidations.requireNonNull;

import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoAtividadeSolicitacao;
import java.time.Instant;
import java.util.UUID;

/** Historico/auditoria de uma solicitacao. Cada evento relevante gera um registro. */
public final class AtividadeSolicitacao {

  private final UUID id;
  private final UUID solicitacaoId;
  private final TipoAtividadeSolicitacao tipo;
  private final StatusSolicitacao deStatus;
  private final StatusSolicitacao paraStatus;
  private final String comentario;
  private final UUID autorUsuarioId;
  private final Instant criadaEm;

  public AtividadeSolicitacao(
      final UUID id,
      final UUID solicitacaoId,
      final TipoAtividadeSolicitacao tipo,
      final StatusSolicitacao deStatus,
      final StatusSolicitacao paraStatus,
      final String comentario,
      final UUID autorUsuarioId,
      final Instant criadaEm) {
    this.id = requireNonNull(id, "id");
    this.solicitacaoId = requireNonNull(solicitacaoId, "solicitacaoId");
    this.tipo = requireNonNull(tipo, "tipo");
    this.deStatus = deStatus;
    this.paraStatus = paraStatus;
    this.comentario = optionalTrimToNull(comentario);
    this.autorUsuarioId = requireNonNull(autorUsuarioId, "autorUsuarioId");
    this.criadaEm = requireNonNull(criadaEm, "criadaEm");
  }

  /** Registra abertura de solicitacao. */
  public static AtividadeSolicitacao abertura(
      final UUID solicitacaoId, final UUID autorId, final Instant agora) {
    return new AtividadeSolicitacao(
        UUID.randomUUID(),
        solicitacaoId,
        TipoAtividadeSolicitacao.ABERTURA,
        null,
        StatusSolicitacao.A_FAZER,
        null,
        autorId,
        agora);
  }

  /** Registra atribuicao de responsavel. */
  public static AtividadeSolicitacao atribuicao(
      final UUID solicitacaoId, final UUID autorId, final String comentario, final Instant agora) {
    return new AtividadeSolicitacao(
        UUID.randomUUID(),
        solicitacaoId,
        TipoAtividadeSolicitacao.ATRIBUICAO,
        null,
        null,
        comentario,
        autorId,
        agora);
  }

  /** Registra mudanca de status no Kanban. */
  public static AtividadeSolicitacao mudancaStatus(
      final UUID solicitacaoId,
      final StatusSolicitacao de,
      final StatusSolicitacao para,
      final UUID autorId,
      final Instant agora) {
    return new AtividadeSolicitacao(
        UUID.randomUUID(),
        solicitacaoId,
        TipoAtividadeSolicitacao.MUDANCA_STATUS,
        de,
        para,
        null,
        autorId,
        agora);
  }

  /** Registra comentario avulso. */
  public static AtividadeSolicitacao comentario(
      final UUID solicitacaoId, final String comentario, final UUID autorId, final Instant agora) {
    return new AtividadeSolicitacao(
        UUID.randomUUID(),
        solicitacaoId,
        TipoAtividadeSolicitacao.COMENTARIO,
        null,
        null,
        comentario,
        autorId,
        agora);
  }

  /** Registra evidencia adicionada. */
  public static AtividadeSolicitacao evidenciaAdicionada(
      final UUID solicitacaoId, final UUID autorId, final Instant agora) {
    return new AtividadeSolicitacao(
        UUID.randomUUID(),
        solicitacaoId,
        TipoAtividadeSolicitacao.EVIDENCIA_ADICIONADA,
        null,
        null,
        null,
        autorId,
        agora);
  }

  public UUID getId() {
    return id;
  }

  public UUID getSolicitacaoId() {
    return solicitacaoId;
  }

  public TipoAtividadeSolicitacao getTipo() {
    return tipo;
  }

  public StatusSolicitacao getDeStatus() {
    return deStatus;
  }

  public StatusSolicitacao getParaStatus() {
    return paraStatus;
  }

  public String getComentario() {
    return comentario;
  }

  public UUID getAutorUsuarioId() {
    return autorUsuarioId;
  }

  public Instant getCriadaEm() {
    return criadaEm;
  }
}
