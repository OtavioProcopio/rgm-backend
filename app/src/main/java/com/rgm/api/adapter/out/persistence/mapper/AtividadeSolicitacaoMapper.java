package com.rgm.api.adapter.out.persistence.mapper;

import com.rgm.api.adapter.out.persistence.entity.AtividadeSolicitacaoJpaEntity;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;

public final class AtividadeSolicitacaoMapper {

  private AtividadeSolicitacaoMapper() {}

  public static AtividadeSolicitacaoJpaEntity toJpa(final AtividadeSolicitacao a) {
    return new AtividadeSolicitacaoJpaEntity(
        a.getId(),
        a.getSolicitacaoId(),
        a.getTipo(),
        a.getDeStatus(),
        a.getParaStatus(),
        a.getComentario(),
        a.getAutorUsuarioId(),
        a.getCriadaEm());
  }

  public static AtividadeSolicitacao toDomain(final AtividadeSolicitacaoJpaEntity e) {
    return new AtividadeSolicitacao(
        e.getId(),
        e.getSolicitacaoId(),
        e.getTipo(),
        e.getDeStatus(),
        e.getParaStatus(),
        e.getComentario(),
        e.getAutorUsuarioId(),
        e.getCriadaEm());
  }
}
