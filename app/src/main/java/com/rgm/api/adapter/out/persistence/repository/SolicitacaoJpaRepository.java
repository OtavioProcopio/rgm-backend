package com.rgm.api.adapter.out.persistence.repository;

import com.rgm.api.adapter.out.persistence.entity.SolicitacaoJpaEntity;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SolicitacaoJpaRepository extends JpaRepository<SolicitacaoJpaEntity, UUID> {

  /** SOL-007: prazo-limite = criada_em + horas de SLA da prioridade atual. */
  String PRAZO_LIMITE_EXPR =
      "(s.criada_em + (CASE s.prioridade "
          + "WHEN 'URGENTE' THEN INTERVAL '4 hours' "
          + "WHEN 'ALTA' THEN INTERVAL '24 hours' "
          + "WHEN 'MEDIA' THEN INTERVAL '72 hours' "
          + "WHEN 'BAIXA' THEN INTERVAL '168 hours' END))";

  /** issue #81: filtro por atrasada — espelha Solicitacao#isAtrasada em SQL. */
  String ATRASADA_FILTRO =
      "(CAST(:atrasada AS boolean) IS NULL OR ("
          + "s.prioridade IS NOT NULL AND ("
          + "CASE s.status "
          + "WHEN 'CANCELADA' THEN false "
          + "WHEN 'CONCLUIDA' THEN s.concluida_em > "
          + PRAZO_LIMITE_EXPR
          + " "
          + "ELSE now() > "
          + PRAZO_LIMITE_EXPR
          + " "
          + "END) = :atrasada))";

  /** Data de encerramento: conclusao das concluidas, cancelamento das canceladas. */
  String ENCERRADA_EM_EXPR = "COALESCE(s.concluida_em, s.cancelada_em)";

  /** "Em aberto" espelha StatusSolicitacao#isNaoTerminal; desligado nao filtra. */
  String EM_ABERTO_FILTRO =
      "(:emAberto = false OR s.status IN ('A_FAZER', 'EM_ANDAMENTO', 'EM_VALIDACAO'))";

  boolean existsByModeloIdAndStatusIn(UUID modeloId, List<StatusSolicitacao> statuses);

  boolean existsByModeloId(UUID modeloId);

  boolean existsByAbertaPorUsuarioId(UUID abertaPorUsuarioId);

  List<SolicitacaoJpaEntity> findByModeloId(UUID modeloId);

  Page<SolicitacaoJpaEntity> findByStatus(StatusSolicitacao status, Pageable pageable);

  @Query(
      value =
          "SELECT s.* FROM solicitacoes s "
              + "LEFT JOIN modelos mo ON mo.id = s.modelo_id WHERE "
              + "(CAST(:status AS text) IS NULL OR s.status = :status) AND "
              + "(CAST(:modeloId AS uuid) IS NULL OR s.modelo_id = :modeloId) AND "
              + "(CAST(:tipo AS text) IS NULL OR s.tipo = :tipo) AND "
              + "(CAST(:prioridade AS text) IS NULL OR s.prioridade = :prioridade) AND "
              + "(CAST(:criadaEmInicio AS timestamptz) IS NULL OR s.criada_em >= :criadaEmInicio) AND "
              + "(CAST(:criadaEmFim AS timestamptz) IS NULL OR s.criada_em <= :criadaEmFim) AND "
              + "(CAST(:concluidaEmInicio AS timestamptz) IS NULL OR "
              + ENCERRADA_EM_EXPR
              + " >= :concluidaEmInicio) AND "
              + "(CAST(:concluidaEmFim AS timestamptz) IS NULL OR "
              + ENCERRADA_EM_EXPR
              + " <= :concluidaEmFim) AND "
              + "(CAST(:abertaPorUsuarioId AS uuid) IS NULL OR s.aberta_por_usuario_id = :abertaPorUsuarioId) AND "
              + "(CAST(:responsavelId AS uuid) IS NULL OR EXISTS (SELECT 1 FROM solicitacao_atribuicoes a WHERE a.solicitacao_id = s.id AND a.usuario_id = :responsavelId AND a.removido_em IS NULL)) AND "
              + "(CAST(:maquina AS text) IS NULL OR mo.maquina = :maquina) AND "
              + ATRASADA_FILTRO
              + " AND "
              + EM_ABERTO_FILTRO,
      countQuery =
          "SELECT COUNT(s.*) FROM solicitacoes s "
              + "LEFT JOIN modelos mo ON mo.id = s.modelo_id WHERE "
              + "(CAST(:status AS text) IS NULL OR s.status = :status) AND "
              + "(CAST(:modeloId AS uuid) IS NULL OR s.modelo_id = :modeloId) AND "
              + "(CAST(:tipo AS text) IS NULL OR s.tipo = :tipo) AND "
              + "(CAST(:prioridade AS text) IS NULL OR s.prioridade = :prioridade) AND "
              + "(CAST(:criadaEmInicio AS timestamptz) IS NULL OR s.criada_em >= :criadaEmInicio) AND "
              + "(CAST(:criadaEmFim AS timestamptz) IS NULL OR s.criada_em <= :criadaEmFim) AND "
              + "(CAST(:concluidaEmInicio AS timestamptz) IS NULL OR "
              + ENCERRADA_EM_EXPR
              + " >= :concluidaEmInicio) AND "
              + "(CAST(:concluidaEmFim AS timestamptz) IS NULL OR "
              + ENCERRADA_EM_EXPR
              + " <= :concluidaEmFim) AND "
              + "(CAST(:abertaPorUsuarioId AS uuid) IS NULL OR s.aberta_por_usuario_id = :abertaPorUsuarioId) AND "
              + "(CAST(:responsavelId AS uuid) IS NULL OR EXISTS (SELECT 1 FROM solicitacao_atribuicoes a WHERE a.solicitacao_id = s.id AND a.usuario_id = :responsavelId AND a.removido_em IS NULL)) AND "
              + "(CAST(:maquina AS text) IS NULL OR mo.maquina = :maquina) AND "
              + ATRASADA_FILTRO
              + " AND "
              + EM_ABERTO_FILTRO,
      nativeQuery = true)
  Page<SolicitacaoJpaEntity> findByFilters(
      @org.springframework.data.repository.query.Param("status") String status,
      @org.springframework.data.repository.query.Param("modeloId") UUID modeloId,
      @org.springframework.data.repository.query.Param("tipo") String tipo,
      @org.springframework.data.repository.query.Param("prioridade") String prioridade,
      @org.springframework.data.repository.query.Param("criadaEmInicio") Instant criadaEmInicio,
      @org.springframework.data.repository.query.Param("criadaEmFim") Instant criadaEmFim,
      @org.springframework.data.repository.query.Param("concluidaEmInicio")
          Instant concluidaEmInicio,
      @org.springframework.data.repository.query.Param("concluidaEmFim") Instant concluidaEmFim,
      @org.springframework.data.repository.query.Param("abertaPorUsuarioId")
          UUID abertaPorUsuarioId,
      @org.springframework.data.repository.query.Param("responsavelId") UUID responsavelId,
      @org.springframework.data.repository.query.Param("maquina") String maquina,
      @org.springframework.data.repository.query.Param("atrasada") Boolean atrasada,
      @org.springframework.data.repository.query.Param("emAberto") boolean emAberto,
      Pageable pageable);

  @Query("SELECT s.modeloId, COUNT(s) FROM SolicitacaoJpaEntity s GROUP BY s.modeloId")
  List<Object[]> countGroupByModeloId();

  long countByStatus(StatusSolicitacao status);

  List<SolicitacaoJpaEntity> findByStatus(StatusSolicitacao status);

  List<SolicitacaoJpaEntity> findByCriadaEmBetween(Instant inicio, Instant fim);

  List<SolicitacaoJpaEntity> findByStatusAndCriadaEmBetween(
      StatusSolicitacao status, Instant inicio, Instant fim);

  @Query(
      value =
          "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (s.concluida_em - s.criada_em))), 0) FROM solicitacoes s WHERE s.status = 'CONCLUIDA'",
      nativeQuery = true)
  double getTempoMedioResolucaoSegundos();

  @Query(
      value =
          "WITH intervalos AS ("
              + "  SELECT modelo_id, "
              + "    EXTRACT(EPOCH FROM (criada_em - LAG(criada_em) OVER (PARTITION BY modelo_id ORDER BY criada_em))) AS intervalo_segundos "
              + "  FROM solicitacoes"
              + "), resolucao AS ("
              + "  SELECT modelo_id, AVG(EXTRACT(EPOCH FROM (concluida_em - criada_em))) AS tempo_medio_resolucao "
              + "  FROM solicitacoes WHERE status = 'CONCLUIDA' GROUP BY modelo_id"
              + "), intervalo_medio AS ("
              + "  SELECT modelo_id, AVG(intervalo_segundos) AS intervalo_medio "
              + "  FROM intervalos WHERE intervalo_segundos IS NOT NULL GROUP BY modelo_id"
              + ") "
              + "SELECT m.id AS modelo_id, m.codigo AS codigo, "
              + "  r.tempo_medio_resolucao AS tempo_medio_resolucao, i.intervalo_medio AS intervalo_medio "
              + "FROM modelos m "
              + "JOIN resolucao r ON r.modelo_id = m.id "
              + "LEFT JOIN intervalo_medio i ON i.modelo_id = m.id",
      countQuery =
          "WITH resolucao AS ("
              + "  SELECT modelo_id FROM solicitacoes WHERE status = 'CONCLUIDA' GROUP BY modelo_id"
              + ") "
              + "SELECT COUNT(*) FROM modelos m JOIN resolucao r ON r.modelo_id = m.id",
      nativeQuery = true)
  Page<Object[]> findMetricasPorModelo(Pageable pageable);

  /**
   * Uma linha: total, em aberto, concluidas, canceladas, primeira abertura, ultima abertura e media
   * em segundos do tempo de resolucao das concluidas (nulo sem concluida).
   */
  @Query(
      "SELECT COUNT(s), "
          + "COALESCE(SUM(CASE WHEN s.status IN (com.rgm.api.core.domain.model.enums.StatusSolicitacao.A_FAZER, "
          + "com.rgm.api.core.domain.model.enums.StatusSolicitacao.EM_ANDAMENTO, "
          + "com.rgm.api.core.domain.model.enums.StatusSolicitacao.EM_VALIDACAO) THEN 1 ELSE 0 END), 0), "
          + "COALESCE(SUM(CASE WHEN s.status = com.rgm.api.core.domain.model.enums.StatusSolicitacao.CONCLUIDA THEN 1 ELSE 0 END), 0), "
          + "COALESCE(SUM(CASE WHEN s.status = com.rgm.api.core.domain.model.enums.StatusSolicitacao.CANCELADA THEN 1 ELSE 0 END), 0), "
          + "MIN(s.criadaEm), MAX(s.criadaEm), "
          + "AVG(CASE WHEN s.status = com.rgm.api.core.domain.model.enums.StatusSolicitacao.CONCLUIDA "
          + "THEN ((s.concluidaEm - s.criadaEm) BY SECOND) END) "
          + "FROM SolicitacaoJpaEntity s WHERE s.modeloId = :modeloId")
  List<Object[]> resumirPorModelo(
      @org.springframework.data.repository.query.Param("modeloId") UUID modeloId);
}
