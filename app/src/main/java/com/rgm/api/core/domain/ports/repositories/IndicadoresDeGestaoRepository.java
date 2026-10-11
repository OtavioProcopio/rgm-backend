package com.rgm.api.core.domain.ports.repositories;

import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Indicadores de gestao agregados no banco: nenhuma operacao carrega solicitacoes. Valor sem dado
 * vem nulo (nunca zero) e as contagens trazem sempre o tamanho da amostra.
 */
public interface IndicadoresDeGestaoRepository {

  /** Concluidas na janela com prioridade: total e quantas cumpriram o prazo, por prioridade. */
  List<PrazoPorPrioridade> prazoPorPrioridade(Instant inicio, Instant fim);

  /** Concluidas na janela que nunca foram triadas (ficam fora da taxa de prazo). */
  long concluidasSemPrioridade(Instant inicio, Instant fim);

  /** Tempo de resolucao (abertura ate conclusao) das concluidas na janela. */
  ResolucaoNoPeriodo resolucao(Instant inicio, Instant fim);

  /** Backlog em aberto em {@code agora}: faixas de idade e abertas sem responsavel. */
  BacklogAgora backlog(Instant agora);

  /** Tempo por etapa das passagens que terminaram na janela (so etapas com dado). */
  List<TempoDeEtapa> tempoPorEtapa(Instant inicio, Instant fim);

  /** Enviadas para validacao e devolvidas (Em validacao de volta para Em andamento). */
  Devolucoes devolucoes(Instant inicio, Instant fim);

  /** Por maquina: abertas e em atraso agora e tempo medio das concluidas na janela. */
  List<DesempenhoDaMaquina> porMaquina(Instant inicio, Instant fim, Instant agora);

  /** Modelos com ao menos {@code minimo} solicitacoes de reparo abertas na janela. */
  List<ModeloReincidente> reincidentes(Instant inicio, Instant fim, int minimo);

  /** Solicitacoes distintas em aberto com responsavel. */
  long abertasComResponsavel();

  /** Abertas atribuidas a cada responsavel ativo na atribuicao. */
  List<CargaDoResponsavel> cargaPorResponsavel();

  record PrazoPorPrioridade(PrioridadeSolicitacao prioridade, long concluidas, long noPrazo) {}

  record ResolucaoNoPeriodo(long concluidas, Double medianaSegundos, Double p90Segundos) {}

  record BacklogAgora(
      long abertas,
      long menosDeUmDia,
      long umATresDias,
      long maisDeTresDias,
      long semResponsavel) {}

  /** Etapa: A_FAZER (espera ate iniciar), EM_ANDAMENTO (execucao) ou EM_VALIDACAO. */
  record TempoDeEtapa(String etapa, long amostras, Double medianaSegundos, Double mediaSegundos) {}

  record Devolucoes(long enviadasParaValidacao, long devolucoes) {}

  record DesempenhoDaMaquina(
      String maquina, long abertas, long emAtraso, Double tempoMedioResolucaoSegundos) {}

  record ModeloReincidente(
      UUID modeloId, String codigo, int versao, String maquina, long solicitacoes) {}

  record CargaDoResponsavel(UUID usuarioId, String nome, long abertas) {}
}
