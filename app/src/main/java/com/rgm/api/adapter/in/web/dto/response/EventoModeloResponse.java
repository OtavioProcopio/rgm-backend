package com.rgm.api.adapter.in.web.dto.response;

import com.rgm.api.core.domain.model.aggregates.EventoModelo;
import java.time.Instant;
import java.util.UUID;

public record EventoModeloResponse(
    UUID id,
    UUID modeloId,
    String tipo,
    String titulo,
    String descricao,
    String estadoModeloDescricao,
    boolean defineFotoCapa,
    UUID executadoPorUsuarioId,
    UUID solicitacaoRelacionadaId,
    Instant criadoEm) {

  public static EventoModeloResponse from(final EventoModelo e) {
    return new EventoModeloResponse(
        e.getId(),
        e.getModeloId(),
        e.getTipo().name(),
        e.getTitulo(),
        e.getDescricao(),
        e.getEstadoModeloDescricao(),
        e.isDefineFotoCapa(),
        e.getExecutadoPorUsuarioId(),
        e.getSolicitacaoRelacionadaId(),
        e.getCriadoEm());
  }
}
