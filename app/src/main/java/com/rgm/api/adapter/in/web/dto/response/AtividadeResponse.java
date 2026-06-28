package com.rgm.api.adapter.in.web.dto.response;

import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import java.time.Instant;
import java.util.UUID;

public record AtividadeResponse(
    UUID id,
    UUID solicitacaoId,
    String tipo,
    String deStatus,
    String paraStatus,
    String comentario,
    UUID autorUsuarioId,
    String autorNome,
    Instant criadaEm) {

  public static AtividadeResponse from(final AtividadeSolicitacao a, final String autorNome) {
    return new AtividadeResponse(
        a.getId(),
        a.getSolicitacaoId(),
        a.getTipo().name(),
        a.getDeStatus() != null ? a.getDeStatus().name() : null,
        a.getParaStatus() != null ? a.getParaStatus().name() : null,
        a.getComentario(),
        a.getAutorUsuarioId(),
        autorNome,
        a.getCriadaEm());
  }
}
