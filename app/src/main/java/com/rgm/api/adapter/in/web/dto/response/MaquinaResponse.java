package com.rgm.api.adapter.in.web.dto.response;

import com.rgm.api.core.domain.model.aggregates.Maquina;
import java.time.Instant;
import java.util.UUID;

public record MaquinaResponse(
    UUID id,
    String nome,
    String codigo,
    String descricao,
    boolean ativa,
    Instant criadaEm,
    Instant atualizadaEm) {

  public static MaquinaResponse from(final Maquina m) {
    return new MaquinaResponse(
        m.getId(),
        m.getNome(),
        m.getCodigo(),
        m.getDescricao(),
        m.isAtiva(),
        m.getCriadaEm(),
        m.getAtualizadaEm());
  }
}
