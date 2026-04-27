package com.rgm.api.adapter.in.web.dto.response;

import com.rgm.api.core.domain.model.aggregates.Evidencia;
import java.time.Instant;
import java.util.UUID;

public record EvidenciaResponse(
    UUID id,
    String publicUrl,
    String mimeType,
    String nomeArquivo,
    Integer tamanhoBytes,
    UUID enviadaPorUsuarioId,
    Instant criadaEm) {

  public static EvidenciaResponse from(final Evidencia e) {
    return new EvidenciaResponse(
        e.getId(),
        e.getPublicUrl(),
        e.getMimeType(),
        e.getNomeArquivo(),
        e.getTamanhoBytes(),
        e.getEnviadaPorUsuarioId(),
        e.getCriadaEm());
  }
}
