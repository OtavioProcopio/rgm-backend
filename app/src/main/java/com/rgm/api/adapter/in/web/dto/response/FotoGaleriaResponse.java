package com.rgm.api.adapter.in.web.dto.response;

import com.rgm.api.core.domain.model.aggregates.FotoGaleriaModelo;
import java.time.Instant;
import java.util.UUID;

public record FotoGaleriaResponse(
    UUID id,
    UUID modeloId,
    String publicUrl,
    String identificacao,
    boolean principal,
    UUID enviadaPorUsuarioId,
    Instant criadoEm) {

  public static FotoGaleriaResponse from(final FotoGaleriaModelo f) {
    return new FotoGaleriaResponse(
        f.getId(),
        f.getModeloId(),
        f.getPublicUrl(),
        f.getIdentificacao(),
        f.isPrincipal(),
        f.getEnviadaPorUsuarioId(),
        f.getCriadoEm());
  }
}
