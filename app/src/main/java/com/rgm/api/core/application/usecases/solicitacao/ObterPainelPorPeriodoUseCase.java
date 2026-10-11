package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.ports.repositories.GranularidadeSerie;
import com.rgm.api.core.domain.ports.repositories.PainelAgregado;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Painel de metricas por periodo: o periodo pedido e o anterior, de mesmo tamanho e adjacente, em
 * dias locais da fabrica. So combina linhas agregadas da porta (uma chamada por janela); nunca
 * recebe nem lista solicitacoes.
 */
public final class ObterPainelPorPeriodoUseCase {

  private static final int DIAS_ATE_SERIE_DIARIA = 30;

  private final SolicitacaoRepository solicitacaoRepository;
  private final ZoneId fuso;
  private final Clock relogio;

  public ObterPainelPorPeriodoUseCase(
      final SolicitacaoRepository solicitacaoRepository, final ZoneId fuso, final Clock relogio) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.fuso = fuso;
    this.relogio = relogio;
  }

  public record Input(int dias, UUID modeloId) {}

  public record Ponto(
      String rotulo,
      long criadas,
      long concluidas,
      long canceladas,
      long abertasAoFimDoDia,
      long atrasadasAoFimDoDia) {}

  /** Janela [inicio, fim) com os totais e a serie; o tempo medio e nulo sem concluida nela. */
  public record Janela(
      Instant inicio,
      Instant fim,
      long criadas,
      long concluidas,
      long canceladas,
      Double tempoMedioResolucaoSegundos,
      List<Ponto> serie) {}

  public record Output(Janela periodo, Janela anterior, long abertasAgora, long atrasadasAgora) {}

  public Output execute(final Input input) {
    final Instant agora = relogio.instant();
    final LocalDate hoje = agora.atZone(fuso).toLocalDate();
    final Instant fimAtual = hoje.plusDays(1).atStartOfDay(fuso).toInstant();
    final Instant inicioAtual = hoje.minusDays(input.dias() - 1L).atStartOfDay(fuso).toInstant();
    final Instant inicioAnterior =
        hoje.minusDays(2L * input.dias() - 1).atStartOfDay(fuso).toInstant();
    final GranularidadeSerie granularidade =
        input.dias() > DIAS_ATE_SERIE_DIARIA ? GranularidadeSerie.SEMANA : GranularidadeSerie.DIA;

    final Janela periodo = janela(inicioAtual, fimAtual, input.modeloId(), granularidade, agora);
    final Janela anterior =
        janela(inicioAnterior, inicioAtual, input.modeloId(), granularidade, agora);
    final PainelAgregado.Agora situacao =
        solicitacaoRepository.situacaoAgora(input.modeloId(), agora);
    return new Output(periodo, anterior, situacao.abertas(), situacao.atrasadas());
  }

  private Janela janela(
      final Instant inicio,
      final Instant fim,
      final UUID modeloId,
      final GranularidadeSerie granularidade,
      final Instant agora) {
    final PainelAgregado agregado =
        solicitacaoRepository.agregarPainel(inicio, fim, modeloId, granularidade, fuso, agora);
    final PainelAgregado.Totais totais = agregado.totais();
    final List<Ponto> serie = agregado.serie().stream().map(p -> ponto(p, granularidade)).toList();
    return new Janela(
        inicio,
        fim,
        totais.criadas(),
        totais.concluidas(),
        totais.canceladas(),
        umaCasa(totais.tempoMedioResolucaoSegundos()),
        serie);
  }

  private static Ponto ponto(final PainelAgregado.Ponto p, final GranularidadeSerie granularidade) {
    return new Ponto(
        RotuloDeSerie.de(p.periodo(), granularidade),
        p.criadas(),
        p.concluidas(),
        p.canceladas(),
        p.abertasAoFimDoDia(),
        p.atrasadasAoFimDoDia());
  }

  private static Double umaCasa(final Double segundos) {
    return segundos == null ? null : Math.round(segundos * 10) / 10.0;
  }
}
