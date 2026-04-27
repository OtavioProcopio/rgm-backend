package com.rgm.api.adapter.out.persistence.mapper;

import com.rgm.api.adapter.out.persistence.entity.SolicitacaoJpaEntity;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;

public final class SolicitacaoMapper {

  private SolicitacaoMapper() {}

  public static SolicitacaoJpaEntity toJpa(final Solicitacao s) {
    return new SolicitacaoJpaEntity(
        s.getId(),
        s.getTitulo(),
        s.getDescricao(),
        s.getTipo(),
        s.getStatus(),
        s.getPrioridade(),
        s.getModeloId(),
        s.getAbertaPorUsuarioId(),
        s.getComentarioFinal(),
        s.getCriadaEm(),
        s.getAtualizadaEm(),
        s.getConcluidaEm(),
        s.getCanceladaEm());
  }

  public static Solicitacao toDomain(final SolicitacaoJpaEntity e) {
    return new Solicitacao(
        e.getId(),
        e.getTitulo(),
        e.getDescricao(),
        e.getTipo(),
        e.getStatus(),
        e.getPrioridade(),
        e.getModeloId(),
        e.getAbertaPorUsuarioId(),
        e.getComentarioFinal(),
        e.getCriadaEm(),
        e.getAtualizadaEm(),
        e.getConcluidaEm(),
        e.getCanceladaEm());
  }
}
