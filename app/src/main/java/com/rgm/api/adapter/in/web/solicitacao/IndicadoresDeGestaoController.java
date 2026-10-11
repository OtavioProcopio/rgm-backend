package com.rgm.api.adapter.in.web.solicitacao;

import com.rgm.api.adapter.in.web.ParametrosDeConsulta;
import com.rgm.api.adapter.in.web.dto.response.IndicadoresDeGestaoResponse;
import com.rgm.api.core.application.usecases.solicitacao.ObterIndicadoresDeGestaoUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Indicadores de gestao (prazo, etapas, retrabalho, maquinas, reincidencia e carga). */
@RestController
@RequestMapping("/api/solicitacoes/metricas/gestao")
public class IndicadoresDeGestaoController {

  private final ObterIndicadoresDeGestaoUseCase obterUseCase;

  public IndicadoresDeGestaoController(final ObterIndicadoresDeGestaoUseCase obterUseCase) {
    this.obterUseCase = obterUseCase;
  }

  @GetMapping
  public ResponseEntity<IndicadoresDeGestaoResponse> obter(
      @RequestParam(defaultValue = "30") final int dias) {
    ParametrosDeConsulta.periodoDoPainel(dias);
    final var output = obterUseCase.execute(new ObterIndicadoresDeGestaoUseCase.Input(dias));
    return ResponseEntity.ok(IndicadoresDeGestaoResponse.from(output));
  }
}
