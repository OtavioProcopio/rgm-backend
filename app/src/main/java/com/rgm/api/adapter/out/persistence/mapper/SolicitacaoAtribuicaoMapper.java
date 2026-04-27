package com.rgm.api.adapter.out.persistence.mapper;

import com.rgm.api.adapter.out.persistence.entity.SolicitacaoAtribuicaoJpaEntity;
import com.rgm.api.core.domain.model.entities.SolicitacaoAtribuicao;

public final class SolicitacaoAtribuicaoMapper {

  private SolicitacaoAtribuicaoMapper() {}

  public static SolicitacaoAtribuicaoJpaEntity toJpa(final SolicitacaoAtribuicao a) {
    return new SolicitacaoAtribuicaoJpaEntity(
        a.getId(),
        a.getSolicitacaoId(),
        a.getUsuarioId(),
        a.getAtribuidoPorUsuarioId(),
        a.getAtribuidoEm(),
        a.getRemovidoEm());
  }

  public static SolicitacaoAtribuicao toDomain(final SolicitacaoAtribuicaoJpaEntity e) {
    return new SolicitacaoAtribuicao(
        e.getId(),
        e.getSolicitacaoId(),
        e.getUsuarioId(),
        e.getAtribuidoPorUsuarioId(),
        e.getAtribuidoEm(),
        e.getRemovidoEm());
  }
}
