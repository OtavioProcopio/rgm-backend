package com.rgm.api.adapter.in.web.dto.response;

import com.rgm.api.core.domain.ports.repositories.ResumoModelos;
import java.util.List;

public record ResumoModelosResponse(
    long total,
    long ativos,
    long inativos,
    long comPendenciaAberta,
    List<QuantidadePorMaquinaResponse> porMaquina) {

  public record QuantidadePorMaquinaResponse(String maquina, long quantidade) {}

  public static ResumoModelosResponse from(final ResumoModelos resumo) {
    return new ResumoModelosResponse(
        resumo.total(),
        resumo.ativos(),
        resumo.inativos(),
        resumo.comPendenciaAberta(),
        resumo.porMaquina().stream()
            .map(q -> new QuantidadePorMaquinaResponse(q.maquina(), q.quantidade()))
            .toList());
  }
}
