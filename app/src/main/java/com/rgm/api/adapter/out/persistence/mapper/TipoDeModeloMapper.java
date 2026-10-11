package com.rgm.api.adapter.out.persistence.mapper;

import com.rgm.api.adapter.out.persistence.entity.TipoDeModeloJpaEntity;
import com.rgm.api.core.domain.model.aggregates.TipoDeModelo;

public final class TipoDeModeloMapper {

  private TipoDeModeloMapper() {}

  public static TipoDeModeloJpaEntity toJpa(final TipoDeModelo m) {
    return new TipoDeModeloJpaEntity(
        m.getId(), m.getNome(), m.isAtivo(), m.getCriadoEm(), m.getAtualizadoEm());
  }

  public static TipoDeModelo toDomain(final TipoDeModeloJpaEntity e) {
    return new TipoDeModelo(
        e.getId(), e.getNome(), e.isAtivo(), e.getCriadoEm(), e.getAtualizadoEm());
  }
}
