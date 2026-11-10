package com.rgm.api.adapter.out.persistence.mapper;

import com.rgm.api.adapter.out.persistence.entity.SolicitacaoJpaEntity;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;

public final class SolicitacaoMapper {

  private SolicitacaoMapper() {}

  public static SolicitacaoJpaEntity toJpa(final Solicitacao s) {
    final SolicitacaoJpaEntity e = new SolicitacaoJpaEntity();
    e.setId(s.getId());
    e.setTitulo(s.getTitulo());
    e.setDescricao(s.getDescricao());
    e.setTipo(s.getTipo());
    e.setStatus(s.getStatus());
    e.setPrioridade(s.getPrioridade());
    e.setModeloId(s.getModeloId());
    e.setModeloCodigo(s.getModeloCodigo());
    e.setModeloMaquina(s.getModeloMaquina());
    e.setModeloObservacoes(s.getModeloObservacoes());
    e.setAbertaPorUsuarioId(s.getAbertaPorUsuarioId());
    e.setComentarioFinal(s.getComentarioFinal());
    e.setInstrucao(s.getInstrucao());
    e.setCriadaEm(s.getCriadaEm());
    e.setAtualizadaEm(s.getAtualizadaEm());
    e.setConcluidaEm(s.getConcluidaEm());
    e.setCanceladaEm(s.getCanceladaEm());
    e.setVersion(s.getVersion());
    return e;
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
        .instrucao(e.getInstrucao())
        .criadaEm(e.getCriadaEm())
        .atualizadaEm(e.getAtualizadaEm())
        .concluidaEm(e.getConcluidaEm())
        .canceladaEm(e.getCanceladaEm())
        .version(e.getVersion())
        .build();
  }
}
