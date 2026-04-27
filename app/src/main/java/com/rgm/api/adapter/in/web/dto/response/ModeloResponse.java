package com.rgm.api.adapter.in.web.dto.response;

import com.rgm.api.core.domain.model.aggregates.Modelo;
import java.time.Instant;
import java.util.UUID;

public record ModeloResponse(
    UUID id,
    String codigo,
    int versao,
    String descricao,
    String observacoes,
    String fotoUrl,
    boolean ativo,
    UUID maquinaId,
    boolean temPendenciaAberta,
    Instant criadoEm,
    Instant atualizadoEm) {

  public static ModeloResponse from(final Modelo m) {
    return new ModeloResponse(
        m.getId(),
        m.getCodigo(),
        m.getVersao(),
        m.getDescricao(),
        m.getObservacoes(),
        m.getFotoUrl(),
        m.isAtivo(),
        m.getMaquinaId(),
        m.isTemPendenciaAberta(),
        m.getCriadoEm(),
        m.getAtualizadoEm());
  }
}
