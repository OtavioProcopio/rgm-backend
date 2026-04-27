package com.rgm.api.adapter.out.persistence.mapper;

import com.rgm.api.adapter.out.persistence.entity.MaquinaJpaEntity;
import com.rgm.api.core.domain.model.aggregates.Maquina;

public final class MaquinaMapper {

  private MaquinaMapper() {}

  public static MaquinaJpaEntity toJpa(final Maquina m) {
    return new MaquinaJpaEntity(
        m.getId(),
        m.getNome(),
        m.getCodigo(),
        m.getDescricao(),
        m.isAtiva(),
        m.getCriadaEm(),
        m.getAtualizadaEm());
  }

  public static Maquina toDomain(final MaquinaJpaEntity e) {
    return new Maquina(
        e.getId(),
        e.getNome(),
        e.getCodigo(),
        e.getDescricao(),
        e.isAtiva(),
        e.getCriadaEm(),
        e.getAtualizadaEm());
  }
}
