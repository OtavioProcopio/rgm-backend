package com.rgm.api.adapter.in.web.dto.response;

import com.rgm.api.core.application.usecases.solicitacao.ObterMetricasSolicitacoesUseCase;
import java.util.Map;

public record MetricasSolicitacaoResponse(
    long totalUsuarios,
    long totalModelos,
    long totalSolicitacoes,
    Map<String, Long> solicitacoesPorStatus,
    long solicitacoesAbertas,
    long solicitacoesPendentes,
    long solicitacoesConcluidas,
    long tempoMedioResolucaoSegundos) {

  public static MetricasSolicitacaoResponse from(
      final ObterMetricasSolicitacoesUseCase.Output output) {
    return new MetricasSolicitacaoResponse(
        output.totalUsuarios(),
        output.totalModelos(),
        output.totalSolicitacoes(),
        output.solicitacoesPorStatus(),
        output.solicitacoesAbertas(),
        output.solicitacoesPendentes(),
        output.solicitacoesConcluidas(),
        output.tempoMedioResolucaoSegundos());
  }
}
