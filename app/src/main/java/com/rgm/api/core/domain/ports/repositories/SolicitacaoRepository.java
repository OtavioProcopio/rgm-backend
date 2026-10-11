package com.rgm.api.core.domain.ports.repositories;

import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.enums.OrdenacaoMetricaModelo;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface SolicitacaoRepository {

  Optional<Solicitacao> findById(UUID id);

  Solicitacao save(Solicitacao solicitacao);

  void deleteById(UUID id);

  boolean existsByModeloIdAndStatusIn(UUID modeloId, List<StatusSolicitacao> statuses);

  boolean existsByModeloId(UUID modeloId);

  boolean existsByAbertaPorUsuarioId(UUID abertaPorUsuarioId);

  List<Solicitacao> findByModeloId(UUID modeloId);

  PageResult<Solicitacao> findAll(int page, int size);

  PageResult<Solicitacao> findByStatus(StatusSolicitacao status, int page, int size);

  /**
   * {@code visivelParaUsuarioId} nulo nao restringe; preenchido traz so as solicitacoes que o
   * usuario abriu ou das quais e responsavel ativo. A ordenacao desempata sempre por criacao e id,
   * para que paginas consecutivas nao repitam nem percam item.
   */
  PageResult<Solicitacao> findByFilters(
      StatusSolicitacao status,
      UUID modeloId,
      TipoSolicitacao tipo,
      PrioridadeSolicitacao prioridade,
      Instant criadaEmInicio,
      Instant criadaEmFim,
      Instant concluidaEmInicio,
      Instant concluidaEmFim,
      UUID abertaPorUsuarioId,
      UUID responsavelId,
      String maquina,
      Boolean atrasada,
      boolean emAberto,
      UUID visivelParaUsuarioId,
      OrdenacaoSolicitacao ordenacao,
      int page,
      int size);

  Map<UUID, Long> countGroupByModeloId();

  long count();

  long countByStatus(StatusSolicitacao status);

  List<Solicitacao> findByStatus(StatusSolicitacao status);

  /**
   * Serie agregada no banco por data de criacao, em [inicio, fim), um ponto por dia local ou por
   * semana (segunda a domingo) no {@code fuso}; so pontos com solicitacao. {@code modeloId} nulo
   * nao filtra. Nunca carrega solicitacoes em memoria.
   */
  List<LinhaDeSerie> agregarSerie(
      Instant inicio, Instant fim, UUID modeloId, GranularidadeSerie granularidade, ZoneId fuso);

  /**
   * Totais e serie do painel na janela [inicio, fim), ambos em meia-noite local do {@code fuso},
   * agregados no banco: criadas pela criacao, concluidas pela conclusao, canceladas pelo
   * cancelamento; a serie traz um ponto por dia ou semana, inclusive os vazios. {@code agora}
   * limita o atraso do dia de hoje. {@code modeloId} nulo nao filtra. Nunca carrega solicitacoes.
   */
  PainelAgregado agregarPainel(
      Instant inicio,
      Instant fim,
      UUID modeloId,
      GranularidadeSerie granularidade,
      ZoneId fuso,
      Instant agora);

  /** Em aberto e em atraso em {@code agora}; {@code modeloId} nulo nao filtra. */
  PainelAgregado.Agora situacaoAgora(UUID modeloId, Instant agora);

  long getTempoMedioResolucaoSegundos();

  /**
   * Ranking de metricas de tempo por modelo (apenas modelos com ao menos 1 solicitacao concluida).
   * Agregado no banco - nunca carrega solicitacoes em memoria.
   */
  PageResult<MetricaModeloRow> findMetricasPorModelo(
      OrdenacaoMetricaModelo sort, boolean ascendente, int page, int size);

  /**
   * Resumo das solicitacoes de um modelo, agregado no banco - nunca carrega solicitacoes em
   * memoria.
   */
  ResumoSolicitacoesModelo resumirPorModelo(UUID modeloId);
}
