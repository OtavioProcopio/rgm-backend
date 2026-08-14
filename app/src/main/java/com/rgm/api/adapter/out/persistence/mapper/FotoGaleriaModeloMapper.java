package com.rgm.api.adapter.out.persistence.mapper;

import com.rgm.api.adapter.out.persistence.entity.FotoGaleriaModeloJpaEntity;
import com.rgm.api.core.domain.model.aggregates.FotoGaleriaModelo;

public final class FotoGaleriaModeloMapper {

  private FotoGaleriaModeloMapper() {}

  public static FotoGaleriaModeloJpaEntity toJpa(final FotoGaleriaModelo f) {
    return new FotoGaleriaModeloJpaEntity(
        f.getId(),
        f.getModeloId(),
        f.getPublicUrl(),
        f.getIdentificacao(),
        f.isPrincipal(),
        f.getEnviadaPorUsuarioId(),
        f.getCriadoEm());
  }

  public static FotoGaleriaModelo toDomain(final FotoGaleriaModeloJpaEntity e) {
    return new FotoGaleriaModelo(
        e.getId(),
        e.getModeloId(),
        e.getPublicUrl(),
        e.getIdentificacao(),
        e.isPrincipal(),
        e.getEnviadaPorUsuarioId(),
        e.getCriadoEm());
  }
}
