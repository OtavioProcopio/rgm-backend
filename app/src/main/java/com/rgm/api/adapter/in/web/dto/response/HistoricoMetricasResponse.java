package com.rgm.api.adapter.in.web.dto.response;

import com.rgm.api.core.application.usecases.solicitacao.ObterHistoricoMetricasUseCase;
import java.util.List;

public record HistoricoMetricasResponse(
    List<PontoDeSerieResponse> series, long slaGlobalMediaHoras, String periodoLabel) {

  public record PontoDeSerieResponse(
      String periodo,
      long total,
      long abertas,
      long concluidas,
      long canceladas,
      long slaMediaHoras) {}

  public static HistoricoMetricasResponse from(
      final ObterHistoricoMetricasUseCase.Output output) {
    final List<PontoDeSerieResponse> series =
        output.series().stream()
            .map(
                p ->
                    new PontoDeSerieResponse(
                        p.periodo(),
                        p.total(),
                        p.abertas(),
                        p.concluidas(),
                        p.canceladas(),
                        p.slaMediaHoras()))
            .toList();
    return new HistoricoMetricasResponse(
        series, output.slaGlobalMediaHoras(), output.periodoLabel());
  }
}
