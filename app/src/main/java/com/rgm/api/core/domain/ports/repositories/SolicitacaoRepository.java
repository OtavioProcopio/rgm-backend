package com.rgm.api.core.domain.ports.repositories;

import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.enums.OrdenacaoMetricaModelo;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import java.time.Instant;
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
   * usuario abriu ou das quais e responsavel ativo.
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
      int page,
      int size);

  Map<UUID, Long> countGroupByModeloId();

  long count();

  long countByStatus(StatusSolicitacao status);

  List<Solicitacao> findByStatus(StatusSolicitacao status);

  List<Solicitacao> findByCriadaEmBetween(Instant inicio, Instant fim);

  List<Solicitacao> findByStatusAndCriadaEmBetween(
      StatusSolicitacao status, Instant inicio, Instant fim);

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
