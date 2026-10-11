package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.ports.repositories.GranularidadeSerie;
import com.rgm.api.core.domain.ports.repositories.LinhaDeSerie;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Historico temporal de metricas por data de criacao. A serie vem agregada do banco (dia ou semana
 * no fuso da fabrica, filtro de modelo na consulta); aqui so se completam os pontos sem solicitacao
 * e se calculam as horas.
 */
public final class ObterHistoricoMetricasUseCase {

  private static final int DIAS_PADRAO = 30;
  private static final int DIAS_MAXIMO = 365;
  private static final int SEGUNDOS_POR_HORA = 3600;

  private final SolicitacaoRepository solicitacaoRepository;
  private final ZoneId fuso;
  private final Clock relogio;

  public ObterHistoricoMetricasUseCase(
      final SolicitacaoRepository solicitacaoRepository, final ZoneId fuso, final Clock relogio) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.fuso = fuso;
    this.relogio = relogio;
  }

  public record Input(int dias, UUID modeloId) {}

  public record PontoDeSerie(
      String periodo,
      long total,
      long abertas,
      long concluidas,
      long canceladas,
      long slaMediaHoras) {}

  public record Output(List<PontoDeSerie> series, long slaGlobalMediaHoras, String periodoLabel) {}

  public Output execute(final Input input) {
    final int dias = input.dias();
    if (dias < 1 || dias > DIAS_MAXIMO) {
      throw new ValidationException(
          "Parâmetro 'dias' deve estar entre 1 e " + DIAS_MAXIMO + " (recebido: " + dias + ")");
    }
    final GranularidadeSerie granularidade =
        dias > DIAS_PADRAO ? GranularidadeSerie.SEMANA : GranularidadeSerie.DIA;
    final LocalDate hoje = relogio.instant().atZone(fuso).toLocalDate();
    final LocalDate primeiroDia = hoje.minusDays(dias);

    final List<LinhaDeSerie> linhas =
        solicitacaoRepository.agregarSerie(
            primeiroDia.atStartOfDay(fuso).toInstant(),
            hoje.plusDays(1).atStartOfDay(fuso).toInstant(),
            input.modeloId(),
            granularidade,
            fuso);
    return montar(linhas, primeiroDia, hoje, granularidade, dias);
  }

  private Output montar(
      final List<LinhaDeSerie> linhas,
      final LocalDate primeiroDia,
      final LocalDate hoje,
      final GranularidadeSerie granularidade,
      final int dias) {
    final Map<LocalDate, LinhaDeSerie> porPeriodo =
        linhas.stream().collect(Collectors.toMap(LinhaDeSerie::periodo, Function.identity()));
    final boolean semanal = granularidade == GranularidadeSerie.SEMANA;
    final List<PontoDeSerie> series = new ArrayList<>();
    LocalDate cursor =
        semanal
            ? primeiroDia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            : primeiroDia;
    while (!cursor.isAfter(hoje)) {
      final String rotulo = RotuloDeSerie.de(cursor, granularidade);
      series.add(ponto(rotulo, porPeriodo.get(cursor)));
      cursor = cursor.plusDays(semanal ? 7 : 1);
    }
    return new Output(series, slaGlobalHoras(linhas), "Últimos " + dias + " dias");
  }

  private static PontoDeSerie ponto(final String rotulo, final LinhaDeSerie linha) {
    if (linha == null) {
      return new PontoDeSerie(rotulo, 0, 0, 0, 0, 0);
    }
    return new PontoDeSerie(
        rotulo,
        linha.total(),
        linha.abertas(),
        linha.concluidas(),
        linha.canceladas(),
        horasInteiras(linha.tempoMedioResolucaoSegundos()));
  }

  private static long horasInteiras(final Double segundos) {
    return segundos == null ? 0 : (long) (segundos / SEGUNDOS_POR_HORA);
  }

  /** Media ponderada pelas concluidas de cada ponto, igual a media de todas as concluidas. */
  private static long slaGlobalHoras(final List<LinhaDeSerie> linhas) {
    double somaSegundos = 0;
    long concluidas = 0;
    for (final LinhaDeSerie linha : linhas) {
      if (linha.tempoMedioResolucaoSegundos() != null) {
        somaSegundos += linha.tempoMedioResolucaoSegundos() * linha.concluidas();
        concluidas += linha.concluidas();
      }
    }
    return concluidas == 0 ? 0 : horasInteiras(somaSegundos / concluidas);
  }
}
