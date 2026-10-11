package com.rgm.api.adapter.in.web.dto.response;

import com.rgm.api.core.domain.ports.repositories.ResumoSolicitacoesModelo;

public record ResumoSolicitacoesModeloResponse(
    long total,
    long emAberto,
    long concluidas,
    long canceladas,
    Double tempoMedioResolucaoSegundos,
    Double intervaloMedioSegundos) {

  public static ResumoSolicitacoesModeloResponse from(final ResumoSolicitacoesModelo resumo) {
    return new ResumoSolicitacoesModeloResponse(
        resumo.total(),
        resumo.emAberto(),
        resumo.concluidas(),
        resumo.canceladas(),
        resumo.tempoMedioResolucaoSegundos(),
        resumo.intervaloMedioSegundos());
  }
}
