package com.rgm.api.adapter.out.persistence.mapper;

import com.rgm.api.adapter.out.persistence.entity.EvidenciaJpaEntity;
import com.rgm.api.core.domain.model.aggregates.Evidencia;

public final class EvidenciaMapper {

  private EvidenciaMapper() {}

  public static EvidenciaJpaEntity toJpa(final Evidencia e) {
    return new EvidenciaJpaEntity(
        e.getId(),
        e.getPublicUrl(),
        e.getMimeType(),
        e.getNomeArquivo(),
        e.getTamanhoBytes(),
        e.getEnviadaPorUsuarioId(),
        e.getCriadaEm());
  }

  public static Evidencia toDomain(final EvidenciaJpaEntity e) {
    return new Evidencia(
        e.getId(),
        e.getPublicUrl(),
        e.getMimeType(),
        e.getNomeArquivo(),
        e.getTamanhoBytes(),
        e.getEnviadaPorUsuarioId(),
        e.getCriadaEm());
  }
}
