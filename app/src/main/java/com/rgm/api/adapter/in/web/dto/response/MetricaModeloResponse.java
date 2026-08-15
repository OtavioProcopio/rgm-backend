package com.rgm.api.adapter.in.web.dto.response;

import com.rgm.api.core.domain.ports.repositories.MetricaModeloRow;
import java.util.UUID;

public record MetricaModeloResponse(
    UUID modeloId,
    String codigo,
    double tempoMedioResolucaoSegundos,
    Double intervaloMedioSegundos) {

  public static MetricaModeloResponse from(final MetricaModeloRow row) {
    return new MetricaModeloResponse(
        row.modeloId(),
        row.codigo(),
        row.tempoMedioResolucaoSegundos(),
        row.intervaloMedioSegundos());
  }
}
