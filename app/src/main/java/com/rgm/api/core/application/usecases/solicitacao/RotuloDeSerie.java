package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.ports.repositories.GranularidadeSerie;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;

/** Rotulo de um ponto de serie: "dd/MM" para dia e "Sem N" (semana ISO) para semana. */
final class RotuloDeSerie {

  private static final DateTimeFormatter DIA_FMT = DateTimeFormatter.ofPattern("dd/MM");

  private RotuloDeSerie() {}

  static String de(final LocalDate periodo, final GranularidadeSerie granularidade) {
    if (granularidade == GranularidadeSerie.SEMANA) {
      return "Sem " + periodo.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }
    return periodo.format(DIA_FMT);
  }
}
