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
        s.getModeloCodigo(),
        s.getModeloMaquina(),
        s.getModeloObservacoes(),
        s.getAbertaPorUsuarioId(),
        s.getComentarioFinal(),
        s.getCriadaEm(),
        s.getAtualizadaEm(),
        s.getConcluidaEm(),
        s.getCanceladaEm(),
        s.getVersion());
  }

  public static Solicitacao toDomain(final SolicitacaoJpaEntity e) {
    return Solicitacao.builder()
        .id(e.getId())
        .titulo(e.getTitulo())
        .descricao(e.getDescricao())
        .tipo(e.getTipo())
        .status(e.getStatus())
        .prioridade(e.getPrioridade())
        .modeloId(e.getModeloId())
        .modeloCodigo(e.getModeloCodigo())
        .modeloMaquina(e.getModeloMaquina())
        .modeloObservacoes(e.getModeloObservacoes())
        .abertaPorUsuarioId(e.getAbertaPorUsuarioId())
        .comentarioFinal(e.getComentarioFinal())
        .criadaEm(e.getCriadaEm())
        .atualizadaEm(e.getAtualizadaEm())
        .concluidaEm(e.getConcluidaEm())
        .canceladaEm(e.getCanceladaEm())
        .version(e.getVersion())
        .build();
  }
}
