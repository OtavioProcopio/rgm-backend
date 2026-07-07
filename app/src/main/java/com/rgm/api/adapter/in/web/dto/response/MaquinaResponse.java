package com.rgm.api.adapter.in.web.dto.response;

import com.rgm.api.core.domain.model.aggregates.Maquina;
import java.time.Instant;
import java.util.UUID;

public record MaquinaResponse(
    UUID id, String nome, boolean ativo, Instant criadoEm, Instant atualizadoEm) {

  public static MaquinaResponse from(final Maquina m) {
    return new MaquinaResponse(
        m.getId(), m.getNome(), m.isAtivo(), m.getCriadoEm(), m.getAtualizadoEm());
  }
}
