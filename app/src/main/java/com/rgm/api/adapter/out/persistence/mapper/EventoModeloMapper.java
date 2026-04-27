package com.rgm.api.adapter.out.persistence.mapper;

import com.rgm.api.adapter.out.persistence.entity.EventoModeloJpaEntity;
import com.rgm.api.core.domain.model.aggregates.EventoModelo;

public final class EventoModeloMapper {

  private EventoModeloMapper() {}

  public static EventoModeloJpaEntity toJpa(final EventoModelo e) {
    return new EventoModeloJpaEntity(
        e.getId(),
        e.getModeloId(),
        e.getTipo(),
        e.getTitulo(),
        e.getDescricao(),
        e.getEstadoModeloDescricao(),
        e.isDefineFotoCapa(),
        e.getExecutadoPorUsuarioId(),
        e.getSolicitacaoRelacionadaId(),
        e.getCriadoEm());
  }

  public static EventoModelo toDomain(final EventoModeloJpaEntity e) {
    return new EventoModelo(
        e.getId(),
        e.getModeloId(),
        e.getTipo(),
        e.getTitulo(),
        e.getDescricao(),
        e.getEstadoModeloDescricao(),
        e.isDefineFotoCapa(),
        e.getExecutadoPorUsuarioId(),
        e.getSolicitacaoRelacionadaId(),
        e.getCriadoEm());
  }
}
