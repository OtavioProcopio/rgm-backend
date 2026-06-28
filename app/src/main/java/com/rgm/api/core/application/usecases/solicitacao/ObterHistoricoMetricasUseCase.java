package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Caso de uso para obter o histórico temporal de métricas de solicitações em buckets. */
public final class ObterHistoricoMetricasUseCase {

  private static final DateTimeFormatter DIA_FMT = DateTimeFormatter.ofPattern("dd/MM");

  private final SolicitacaoRepository solicitacaoRepository;

  public ObterHistoricoMetricasUseCase(final SolicitacaoRepository solicitacaoRepository) {
    this.solicitacaoRepository = solicitacaoRepository;
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
    final int dias = input.dias() <= 0 ? 30 : input.dias();
    final boolean semanal = dias > 30;

    final Instant fim = Instant.now();
    final Instant inicio = fim.minus(dias, ChronoUnit.DAYS);

    final List<Solicitacao> noPeriodo =
        solicitacaoRepository.findByCriadaEmBetween(inicio, fim).stream()
            .filter(s -> input.modeloId() == null || input.modeloId().equals(s.getModeloId()))
            .toList();

    final List<Bucket> buckets = montarBuckets(inicio, fim, semanal);

    for (final Solicitacao s : noPeriodo) {
      final Bucket bucket = bucketDe(buckets, s.getCriadaEm());
      if (bucket == null) {
        continue;
      }
      bucket.total++;
      final StatusSolicitacao status = s.getStatus();
      if (status == StatusSolicitacao.CONCLUIDA) {
        bucket.concluidas++;
        if (s.getCriadaEm() != null && s.getConcluidaEm() != null) {
          bucket.slaTotalSegundos += Duration.between(s.getCriadaEm(), s.getConcluidaEm()).toSeconds();
          bucket.slaContagem++;
        }
      } else if (status == StatusSolicitacao.CANCELADA) {
        bucket.canceladas++;
      } else {
        bucket.abertas++;
      }
    }

    final List<PontoDeSerie> series = new ArrayList<>(buckets.size());
    long slaGlobalSegundos = 0;
    long slaGlobalContagem = 0;
    for (final Bucket b : buckets) {
      final long slaMediaHoras =
          b.slaContagem == 0 ? 0 : (b.slaTotalSegundos / b.slaContagem) / 3600;
      series.add(
          new PontoDeSerie(
              b.label, b.total, b.abertas, b.concluidas, b.canceladas, slaMediaHoras));
      slaGlobalSegundos += b.slaTotalSegundos;
      slaGlobalContagem += b.slaContagem;
    }

    final long slaGlobalMediaHoras =
        slaGlobalContagem == 0 ? 0 : (slaGlobalSegundos / slaGlobalContagem) / 3600;
    final String periodoLabel = "Últimos " + dias + " dias";

    return new Output(series, slaGlobalMediaHoras, periodoLabel);
  }

  private List<Bucket> montarBuckets(final Instant inicio, final Instant fim, final boolean semanal) {
    final List<Bucket> buckets = new ArrayList<>();
    final LocalDate inicioDia = inicio.atZone(ZoneOffset.UTC).toLocalDate();
    final LocalDate fimDia = fim.atZone(ZoneOffset.UTC).toLocalDate();

    if (semanal) {
      LocalDate cursor = inicioDia;
      while (!cursor.isAfter(fimDia)) {
        final LocalDate inicioSemana = cursor;
        final LocalDate fimSemana = cursor.plusDays(6);
        final String label =
            "Sem " + inicioSemana.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        buckets.add(
            new Bucket(
                label,
                inicioSemana.atStartOfDay(ZoneOffset.UTC).toInstant(),
                fimSemana.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        cursor = cursor.plusWeeks(1);
      }
    } else {
      LocalDate cursor = inicioDia;
      while (!cursor.isAfter(fimDia)) {
        buckets.add(
            new Bucket(
                cursor.format(DIA_FMT),
                cursor.atStartOfDay(ZoneOffset.UTC).toInstant(),
                cursor.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        cursor = cursor.plusDays(1);
      }
    }
    return buckets;
  }

  private Bucket bucketDe(final List<Bucket> buckets, final Instant criadaEm) {
    if (criadaEm == null) {
      return null;
    }
    for (final Bucket b : buckets) {
      if (!criadaEm.isBefore(b.inicio) && criadaEm.isBefore(b.fim)) {
        return b;
      }
    }
    return null;
  }

  private static final class Bucket {
    private final String label;
    private final Instant inicio;
    private final Instant fim;
    private long total;
    private long abertas;
    private long concluidas;
    private long canceladas;
    private long slaTotalSegundos;
    private long slaContagem;

    private Bucket(final String label, final Instant inicio, final Instant fim) {
      this.label = label;
      this.inicio = inicio;
      this.fim = fim;
    }
  }
}
