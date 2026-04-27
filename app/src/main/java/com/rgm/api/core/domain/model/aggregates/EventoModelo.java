package com.rgm.api.core.domain.model.aggregates;

import static com.rgm.api.core.domain.validation.DomainValidations.optionalTrimToNull;
import static com.rgm.api.core.domain.validation.DomainValidations.requireNonBlank;
import static com.rgm.api.core.domain.validation.DomainValidations.requireNonNull;

import com.rgm.api.core.domain.model.enums.TipoEventoModelo;
import java.time.Instant;
import java.util.UUID;

/** Evento na timeline/prontuario do Modelo. */
public final class EventoModelo {

  private final UUID id;
  private final UUID modeloId;
  private final TipoEventoModelo tipo;
  private final String titulo;
  private final String descricao;
  private final String estadoModeloDescricao;
  private final boolean defineFotoCapa;
  private final UUID executadoPorUsuarioId;
  private final UUID solicitacaoRelacionadaId;
  private final Instant criadoEm;

  public EventoModelo(
      final UUID id,
      final UUID modeloId,
      final TipoEventoModelo tipo,
      final String titulo,
      final String descricao,
      final String estadoModeloDescricao,
      final boolean defineFotoCapa,
      final UUID executadoPorUsuarioId,
      final UUID solicitacaoRelacionadaId,
      final Instant criadoEm) {
    this.id = requireNonNull(id, "id");
    this.modeloId = requireNonNull(modeloId, "modeloId");
    this.tipo = requireNonNull(tipo, "tipo");
    this.titulo = requireNonBlank(titulo, "titulo");
    this.descricao = requireNonBlank(descricao, "descricao");
    this.estadoModeloDescricao = optionalTrimToNull(estadoModeloDescricao);
    this.defineFotoCapa = defineFotoCapa;
    this.executadoPorUsuarioId = requireNonNull(executadoPorUsuarioId, "executadoPorUsuarioId");
    this.solicitacaoRelacionadaId = solicitacaoRelacionadaId;
    this.criadoEm = requireNonNull(criadoEm, "criadoEm");
  }

  /** Cria um novo evento na timeline do modelo. */
  public static EventoModelo criar(
      final UUID modeloId,
      final TipoEventoModelo tipo,
      final String titulo,
      final String descricao,
      final String estadoModeloDescricao,
      final boolean defineFotoCapa,
      final UUID executadoPorUsuarioId,
      final UUID solicitacaoRelacionadaId,
      final Instant agora) {
    return new EventoModelo(
        UUID.randomUUID(),
        modeloId,
        tipo,
        titulo,
        descricao,
        estadoModeloDescricao,
        defineFotoCapa,
        executadoPorUsuarioId,
        solicitacaoRelacionadaId,
        agora);
  }

  public UUID getId() {
    return id;
  }

  public UUID getModeloId() {
    return modeloId;
  }

  public TipoEventoModelo getTipo() {
    return tipo;
  }

  public String getTitulo() {
    return titulo;
  }

  public String getDescricao() {
    return descricao;
  }

  public String getEstadoModeloDescricao() {
    return estadoModeloDescricao;
  }

  public boolean isDefineFotoCapa() {
    return defineFotoCapa;
  }

  public UUID getExecutadoPorUsuarioId() {
    return executadoPorUsuarioId;
  }

  public UUID getSolicitacaoRelacionadaId() {
    return solicitacaoRelacionadaId;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }
}
