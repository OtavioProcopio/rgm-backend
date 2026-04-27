package com.rgm.api.adapter.out.persistence.mapper;

import com.rgm.api.adapter.out.persistence.entity.ModeloJpaEntity;
import com.rgm.api.core.domain.model.aggregates.Modelo;

public final class ModeloMapper {

  private ModeloMapper() {}

  public static ModeloJpaEntity toJpa(final Modelo m) {
    return new ModeloJpaEntity(
        m.getId(),
        m.getCodigo(),
        m.getVersao(),
        m.getDescricao(),
        m.getObservacoes(),
        m.getFotoUrl(),
        m.getFotoAtualizadaEm(),
        m.getEstadoAtualDescricao(),
        m.getEstadoAtualAtualizadoEm(),
        m.isAtivo(),
        m.getMaquinaId(),
        m.isTemPendenciaAberta(),
        m.getCriadoEm(),
        m.getAtualizadoEm());
  }

  public static Modelo toDomain(final ModeloJpaEntity e) {
    return new Modelo(
        e.getId(),
        e.getCodigo(),
        e.getVersao(),
        e.getDescricao(),
        e.getObservacoes(),
        e.getFotoUrl(),
        e.getFotoAtualizadaEm(),
        e.getEstadoAtualDescricao(),
        e.getEstadoAtualAtualizadoEm(),
        e.isAtivo(),
        e.getMaquinaId(),
        e.isTemPendenciaAberta(),
        e.getCriadoEm(),
        e.getAtualizadoEm());
  }
}
