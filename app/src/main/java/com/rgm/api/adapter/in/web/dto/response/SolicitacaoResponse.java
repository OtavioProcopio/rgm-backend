package com.rgm.api.adapter.in.web.dto.response;

import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.enums.AcaoSolicitacao;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record SolicitacaoResponse(
    UUID id,
    String titulo,
    String descricao,
    String tipo,
    String status,
    String prioridade,
    UUID modeloId,
    String modeloCodigo,
    String modeloMaquina,
    String modeloObservacoes,
    UUID abertaPorUsuarioId,
    String comentarioFinal,
    Instant criadaEm,
    Instant atualizadaEm,
    Instant concluidaEm,
    Instant canceladaEm,
    List<UUID> responsavelIds,
    Instant prazoLimite,
    Long tempoRestanteSegundos,
    boolean atrasada,
    Long tempoResolucaoSegundos,
    List<String> acoesPermitidas) {

  public static SolicitacaoResponse from(final Solicitacao s) {
    return from(s, List.of());
  }

  /** Sem acoes calculadas: `acoesPermitidas` sai nulo (listagens, eventos e respostas de acao). */
  public static SolicitacaoResponse from(final Solicitacao s, final List<UUID> responsaveis) {
    return from(s, responsaveis, null);
  }

  public static SolicitacaoResponse from(
      final Solicitacao s,
      final List<UUID> responsaveis,
      final Set<AcaoSolicitacao> acoesPermitidas) {
    final Instant agora = Instant.now();
    return new SolicitacaoResponse(
        s.getId(),
        s.getTitulo(),
        s.getDescricao(),
        s.getTipo().name(),
        s.getStatus().name(),
        s.getPrioridade() != null ? s.getPrioridade().name() : null,
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
        responsaveis,
        s.getPrazoLimite(),
        s.getTempoRestanteSegundos(agora),
        s.isAtrasada(agora),
        s.getTempoResolucaoSegundos(),
        acoesPermitidas == null ? null : acoesPermitidas.stream().map(Enum::name).toList());
  }
}
