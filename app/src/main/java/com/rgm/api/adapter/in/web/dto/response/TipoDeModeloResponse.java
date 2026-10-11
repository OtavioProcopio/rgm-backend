package com.rgm.api.adapter.in.web.dto.response;

import com.rgm.api.core.domain.model.aggregates.TipoDeModelo;
import java.time.Instant;
import java.util.UUID;

public record TipoDeModeloResponse(
    UUID id, String nome, boolean ativo, Instant criadoEm, Instant atualizadoEm) {

  public static TipoDeModeloResponse from(final TipoDeModelo m) {
    return new TipoDeModeloResponse(
        m.getId(), m.getNome(), m.isAtivo(), m.getCriadoEm(), m.getAtualizadoEm());
  }
}
