package com.rgm.api.core.domain.ports.repositories;

import java.time.LocalDate;

/**
 * Linha agregada de uma serie por data de criacao. {@code periodo} e o dia local, ou a
 * segunda-feira da semana; {@code tempoMedioResolucaoSegundos} e nulo sem solicitacao concluida no
 * periodo.
 */
public record LinhaDeSerie(
    LocalDate periodo,
    long total,
    long abertas,
    long concluidas,
    long canceladas,
    Double tempoMedioResolucaoSegundos) {}
