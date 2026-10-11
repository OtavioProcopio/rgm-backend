package com.rgm.api.adapter.in.web.dto.response;

import com.rgm.api.core.application.usecases.solicitacao.ObterIndicadoresDeGestaoUseCase;
import com.rgm.api.core.application.usecases.solicitacao.ObterIndicadoresDeGestaoUseCase.Backlog;
import com.rgm.api.core.application.usecases.solicitacao.ObterIndicadoresDeGestaoUseCase.Carga;
import com.rgm.api.core.application.usecases.solicitacao.ObterIndicadoresDeGestaoUseCase.Devolucao;
import com.rgm.api.core.application.usecases.solicitacao.ObterIndicadoresDeGestaoUseCase.Etapas;
import com.rgm.api.core.application.usecases.solicitacao.ObterIndicadoresDeGestaoUseCase.Maquina;
import com.rgm.api.core.application.usecases.solicitacao.ObterIndicadoresDeGestaoUseCase.Prazo;
import com.rgm.api.core.application.usecases.solicitacao.ObterIndicadoresDeGestaoUseCase.Reincidente;
import com.rgm.api.core.application.usecases.solicitacao.ObterIndicadoresDeGestaoUseCase.Resolucao;
import java.time.Instant;
import java.util.List;

/** Indicadores de gestao do periodo: valores sem dado vem nulos, nunca zero. */
public record IndicadoresDeGestaoResponse(
    int dias,
    Instant inicio,
    Instant fim,
    Prazo prazo,
    Resolucao resolucao,
    Backlog backlog,
    Etapas etapas,
    Devolucao devolucao,
    List<Maquina> porMaquina,
    List<Reincidente> reincidentes,
    Carga carga) {

  public static IndicadoresDeGestaoResponse from(
      final ObterIndicadoresDeGestaoUseCase.Output output) {
    return new IndicadoresDeGestaoResponse(
        output.dias(),
        output.inicio(),
        output.fim(),
        output.prazo(),
        output.resolucao(),
        output.backlog(),
        output.etapas(),
        output.devolucao(),
        output.porMaquina(),
        output.reincidentes(),
        output.carga());
  }
}
