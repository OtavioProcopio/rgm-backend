package com.rgm.api.core.domain.ports.repositories;

import java.util.UUID;

/**
 * Linha agregada do ranking de metricas de tempo por modelo. `intervaloMedioSegundos` e nulo quando
 * o modelo tem menos de 2 solicitacoes (nao ha um segundo ponto para calcular intervalo).
 */
public record MetricaModeloRow(
    UUID modeloId,
    String codigo,
    double tempoMedioResolucaoSegundos,
    Double intervaloMedioSegundos) {}
