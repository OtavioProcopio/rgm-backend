package com.rgm.api.adapter.in.web.dashboard;

import com.rgm.api.core.application.usecases.dashboard.ObterMetricasDashboardUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
  private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

  private final ObterMetricasDashboardUseCase obterMetricasUseCase;

  public DashboardController(final ObterMetricasDashboardUseCase obterMetricasUseCase) {
    this.obterMetricasUseCase = obterMetricasUseCase;
  }

  @GetMapping("/metricas")
  public ResponseEntity<ObterMetricasDashboardUseCase.DashboardMetricas> obterMetricas() {
    log.info("DashboardController.obterMetricas iniciado");
    final var metrics = obterMetricasUseCase.execute();
    return ResponseEntity.ok(metrics);
  }
}
