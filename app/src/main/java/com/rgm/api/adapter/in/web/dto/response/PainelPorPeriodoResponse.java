package com.rgm.api.adapter.in.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rgm.api.core.application.usecases.solicitacao.ObterPainelPorPeriodoUseCase;
import java.time.Instant;
import java.util.List;

/**
 * Painel por periodo: {@code periodo} e {@code anterior} tem a mesma forma; so o periodo atual traz
 * {@code abertasAgora} e {@code atrasadasAgora} (no anterior os campos nao aparecem).
 */
public record PainelPorPeriodoResponse(JanelaResponse periodo, JanelaResponse anterior) {

  public record JanelaResponse(
      Instant inicio,
      Instant fim,
      long criadas,
      long concluidas,
      long canceladas,
      Double tempoMedioResolucaoSegundos,
      @JsonInclude(JsonInclude.Include.NON_NULL) Long abertasAgora,
      @JsonInclude(JsonInclude.Include.NON_NULL) Long atrasadasAgora,
      List<PontoResponse> serie) {}

  public record PontoResponse(
      String rotulo,
      long criadas,
      long concluidas,
      long canceladas,
      long abertasAoFimDoDia,
      long atrasadasAoFimDoDia) {}

  public static PainelPorPeriodoResponse from(final ObterPainelPorPeriodoUseCase.Output output) {
    return new PainelPorPeriodoResponse(
        janela(output.periodo(), output.abertasAgora(), output.atrasadasAgora()),
        janela(output.anterior(), null, null));
  }

  private static JanelaResponse janela(
      final ObterPainelPorPeriodoUseCase.Janela janela,
      final Long abertasAgora,
      final Long atrasadasAgora) {
    return new JanelaResponse(
        janela.inicio(),
        janela.fim(),
        janela.criadas(),
        janela.concluidas(),
        janela.canceladas(),
        janela.tempoMedioResolucaoSegundos(),
        abertasAgora,
        atrasadasAgora,
        janela.serie().stream().map(PainelPorPeriodoResponse::ponto).toList());
  }

  private static PontoResponse ponto(final ObterPainelPorPeriodoUseCase.Ponto ponto) {
    return new PontoResponse(
        ponto.rotulo(),
        ponto.criadas(),
        ponto.concluidas(),
        ponto.canceladas(),
        ponto.abertasAoFimDoDia(),
        ponto.atrasadasAoFimDoDia());
  }
}
