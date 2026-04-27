package com.rgm.api.adapter.in.web.dto.response;

import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import java.time.Instant;
import java.util.UUID;

public record SolicitacaoResponse(
    UUID id,
    String titulo,
    String descricao,
    String tipo,
    String status,
    String prioridade,
    UUID modeloId,
    UUID abertaPorUsuarioId,
    String comentarioFinal,
    Instant criadaEm,
    Instant atualizadaEm,
    Instant concluidaEm,
    Instant canceladaEm) {

  public static SolicitacaoResponse from(final Solicitacao s) {
    return new SolicitacaoResponse(
        s.getId(),
        s.getTitulo(),
        s.getDescricao(),
        s.getTipo().name(),
        s.getStatus().name(),
        s.getPrioridade() != null ? s.getPrioridade().name() : null,
        s.getModeloId(),
        s.getAbertaPorUsuarioId(),
        s.getComentarioFinal(),
        s.getCriadaEm(),
        s.getAtualizadaEm(),
        s.getConcluidaEm(),
        s.getCanceladaEm());
  }
}
