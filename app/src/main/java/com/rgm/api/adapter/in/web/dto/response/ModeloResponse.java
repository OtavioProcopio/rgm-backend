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
    String fotoCapaUrl,
    boolean ativo,
    String maquina,
    boolean temPendenciaAberta,
    Instant criadoEm,
    Instant atualizadoEm) {

  /** Sem foto de capa resolvida (uso interno, ex.: apos criar/editar/ativar/desativar). */
  public static ModeloResponse from(final Modelo m) {
    return from(m, null);
  }

  /** Com a URL da foto principal da galeria ja resolvida (uso em listagens/detalhe). */
  public static ModeloResponse from(final Modelo m, final String fotoCapaUrl) {
    return new ModeloResponse(
        m.getId(),
        m.getCodigo(),
        m.getVersao(),
        m.getDescricao(),
        m.getObservacoes(),
        fotoCapaUrl,
        m.isAtivo(),
        m.getMaquina(),
        m.isTemPendenciaAberta(),
        m.getCriadoEm(),
        m.getAtualizadoEm());
  }
}
