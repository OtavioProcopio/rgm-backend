package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.ports.repositories.IndicadoresDeGestaoRepository;
import com.rgm.api.core.domain.ports.repositories.IndicadoresDeGestaoRepository.DesempenhoDaMaquina;
import com.rgm.api.core.domain.ports.repositories.IndicadoresDeGestaoRepository.PrazoPorPrioridade;
import com.rgm.api.core.domain.ports.repositories.IndicadoresDeGestaoRepository.TempoDeEtapa;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Indicadores de gestao dos ultimos N dias locais da fabrica (mesma janela do painel por periodo).
 * So combina linhas agregadas da porta; nunca recebe nem lista solicitacoes. Valor sem dado fica
 * nulo, nunca zero.
 */
public final class ObterIndicadoresDeGestaoUseCase {

  /** Solicitacoes de reparo no periodo a partir das quais o modelo e reincidente. */
  public static final int LIMITE_DE_REINCIDENCIA = 3;

  private static final List<PrioridadeSolicitacao> ORDEM_DE_PRIORIDADE =
      List.of(
          PrioridadeSolicitacao.URGENTE,
          PrioridadeSolicitacao.ALTA,
          PrioridadeSolicitacao.MEDIA,
          PrioridadeSolicitacao.BAIXA);

  private final IndicadoresDeGestaoRepository repositorio;
  private final ZoneId fuso;
  private final Clock relogio;

  public ObterIndicadoresDeGestaoUseCase(
      final IndicadoresDeGestaoRepository repositorio, final ZoneId fuso, final Clock relogio) {
    this.repositorio = repositorio;
    this.fuso = fuso;
    this.relogio = relogio;
  }

  public record Input(int dias) {}

  public record PrazoDaPrioridade(
      String prioridade, long concluidas, long noPrazo, Double percentualNoPrazo) {}

  public record Prazo(
      long concluidasComPrioridade,
      long noPrazo,
      Double percentualNoPrazo,
      long concluidasSemPrioridade,
      List<PrazoDaPrioridade> porPrioridade) {}

  public record Resolucao(long concluidas, Double medianaSegundos, Double p90Segundos) {}

  public record Backlog(
      long abertas,
      long menosDeUmDia,
      long umATresDias,
      long maisDeTresDias,
      long semResponsavel) {}

  public record Etapa(long amostras, Double medianaSegundos, Double mediaSegundos) {}

  public record Etapas(Etapa esperaAteIniciar, Etapa execucao, Etapa esperaDeValidacao) {}

  public record Devolucao(long enviadasParaValidacao, long devolucoes, Double taxa) {}

  public record Maquina(
      String maquina, long abertas, long emAtraso, Double tempoMedioResolucaoSegundos) {}

  public record Reincidente(
      UUID modeloId, String codigo, int versao, String maquina, long solicitacoes) {}

  public record Responsavel(UUID usuarioId, String nome, long abertas) {}

  /**
   * {@code solicitacoesDistintas}: a soma de {@code abertas} pode passar dela (co-responsaveis).
   */
  public record Carga(long solicitacoesDistintas, List<Responsavel> responsaveis) {}

  public record Output(
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
      Carga carga) {}

  public Output execute(final Input input) {
    final Instant agora = relogio.instant();
    final LocalDate hoje = agora.atZone(fuso).toLocalDate();
    final Instant fim = hoje.plusDays(1).atStartOfDay(fuso).toInstant();
    final Instant inicio = hoje.minusDays(input.dias() - 1L).atStartOfDay(fuso).toInstant();

    final var resolucao = repositorio.resolucao(inicio, fim);
    final var backlog = repositorio.backlog(agora);
    final var devolucoes = repositorio.devolucoes(inicio, fim);

    return new Output(
        input.dias(),
        inicio,
        fim,
        prazo(inicio, fim),
        new Resolucao(resolucao.concluidas(), resolucao.medianaSegundos(), resolucao.p90Segundos()),
        new Backlog(
            backlog.abertas(),
            backlog.menosDeUmDia(),
            backlog.umATresDias(),
            backlog.maisDeTresDias(),
            backlog.semResponsavel()),
        etapas(repositorio.tempoPorEtapa(inicio, fim)),
        new Devolucao(
            devolucoes.enviadasParaValidacao(),
            devolucoes.devolucoes(),
            percentual(devolucoes.devolucoes(), devolucoes.enviadasParaValidacao())),
        repositorio.porMaquina(inicio, fim, agora).stream().map(this::maquina).toList(),
        repositorio.reincidentes(inicio, fim, LIMITE_DE_REINCIDENCIA).stream()
            .map(
                r ->
                    new Reincidente(
                        r.modeloId(), r.codigo(), r.versao(), r.maquina(), r.solicitacoes()))
            .toList(),
        new Carga(
            repositorio.abertasComResponsavel(),
            repositorio.cargaPorResponsavel().stream()
                .map(c -> new Responsavel(c.usuarioId(), c.nome(), c.abertas()))
                .toList()));
  }

  private Prazo prazo(final Instant inicio, final Instant fim) {
    final List<PrazoPorPrioridade> linhas = repositorio.prazoPorPrioridade(inicio, fim);
    final List<PrazoDaPrioridade> porPrioridade =
        ORDEM_DE_PRIORIDADE.stream()
            .map(
                p -> {
                  final var linha =
                      linhas.stream().filter(l -> l.prioridade() == p).findFirst().orElse(null);
                  final long concluidas = linha == null ? 0 : linha.concluidas();
                  final long noPrazo = linha == null ? 0 : linha.noPrazo();
                  return new PrazoDaPrioridade(
                      p.name(), concluidas, noPrazo, percentual(noPrazo, concluidas));
                })
            .toList();
    final long concluidas = linhas.stream().mapToLong(PrazoPorPrioridade::concluidas).sum();
    final long noPrazo = linhas.stream().mapToLong(PrazoPorPrioridade::noPrazo).sum();
    return new Prazo(
        concluidas,
        noPrazo,
        percentual(noPrazo, concluidas),
        repositorio.concluidasSemPrioridade(inicio, fim),
        porPrioridade);
  }

  private static Etapas etapas(final List<TempoDeEtapa> linhas) {
    return new Etapas(
        etapa(linhas, "A_FAZER"), etapa(linhas, "EM_ANDAMENTO"), etapa(linhas, "EM_VALIDACAO"));
  }

  private static Etapa etapa(final List<TempoDeEtapa> linhas, final String nome) {
    return linhas.stream()
        .filter(l -> l.etapa().equals(nome))
        .findFirst()
        .map(l -> new Etapa(l.amostras(), l.medianaSegundos(), l.mediaSegundos()))
        .orElse(new Etapa(0, null, null));
  }

  private Maquina maquina(final DesempenhoDaMaquina m) {
    return new Maquina(m.maquina(), m.abertas(), m.emAtraso(), m.tempoMedioResolucaoSegundos());
  }

  /** Porcentagem de 0 a 100 com uma casa; nulo quando nao ha base. */
  private static Double percentual(final long parte, final long total) {
    if (total == 0) {
      return null;
    }
    return Math.round(1000.0 * parte / total) / 10.0;
  }
}
