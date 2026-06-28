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

  boolean existsByModeloIdAndStatusIn(UUID modeloId, List<StatusSolicitacao> statuses);

  boolean existsByModeloId(UUID modeloId);

  boolean existsByAbertaPorUsuarioId(UUID abertaPorUsuarioId);

  List<SolicitacaoJpaEntity> findByModeloId(UUID modeloId);

  Page<SolicitacaoJpaEntity> findByStatus(StatusSolicitacao status, Pageable pageable);

  @Query(
      value =
          "SELECT s.* FROM solicitacoes s WHERE "
              + "(CAST(:status AS text) IS NULL OR s.status = :status) AND "
              + "(CAST(:modeloId AS uuid) IS NULL OR s.modelo_id = :modeloId) AND "
              + "(CAST(:tipo AS text) IS NULL OR s.tipo = :tipo) AND "
              + "(CAST(:prioridade AS text) IS NULL OR s.prioridade = :prioridade) AND "
              + "(CAST(:criadaEmInicio AS timestamptz) IS NULL OR s.criada_em >= :criadaEmInicio) AND "
              + "(CAST(:criadaEmFim AS timestamptz) IS NULL OR s.criada_em <= :criadaEmFim) AND "
              + "(CAST(:abertaPorUsuarioId AS uuid) IS NULL OR s.aberta_por_usuario_id = :abertaPorUsuarioId) AND "
              + "(CAST(:responsavelId AS uuid) IS NULL OR EXISTS (SELECT 1 FROM solicitacao_atribuicoes a WHERE a.solicitacao_id = s.id AND a.usuario_id = :responsavelId AND a.removido_em IS NULL))",
      countQuery =
          "SELECT COUNT(s.*) FROM solicitacoes s WHERE "
              + "(CAST(:status AS text) IS NULL OR s.status = :status) AND "
              + "(CAST(:modeloId AS uuid) IS NULL OR s.modelo_id = :modeloId) AND "
              + "(CAST(:tipo AS text) IS NULL OR s.tipo = :tipo) AND "
              + "(CAST(:prioridade AS text) IS NULL OR s.prioridade = :prioridade) AND "
              + "(CAST(:criadaEmInicio AS timestamptz) IS NULL OR s.criada_em >= :criadaEmInicio) AND "
              + "(CAST(:criadaEmFim AS timestamptz) IS NULL OR s.criada_em <= :criadaEmFim) AND "
              + "(CAST(:abertaPorUsuarioId AS uuid) IS NULL OR s.aberta_por_usuario_id = :abertaPorUsuarioId) AND "
              + "(CAST(:responsavelId AS uuid) IS NULL OR EXISTS (SELECT 1 FROM solicitacao_atribuicoes a WHERE a.solicitacao_id = s.id AND a.usuario_id = :responsavelId AND a.removido_em IS NULL))",
      nativeQuery = true)
  Page<SolicitacaoJpaEntity> findByFilters(
      @org.springframework.data.repository.query.Param("status") String status,
      @org.springframework.data.repository.query.Param("modeloId") UUID modeloId,
      @org.springframework.data.repository.query.Param("tipo") String tipo,
      @org.springframework.data.repository.query.Param("prioridade") String prioridade,
      @org.springframework.data.repository.query.Param("criadaEmInicio") Instant criadaEmInicio,
      @org.springframework.data.repository.query.Param("criadaEmFim") Instant criadaEmFim,
      @org.springframework.data.repository.query.Param("abertaPorUsuarioId")
          UUID abertaPorUsuarioId,
      @org.springframework.data.repository.query.Param("responsavelId") UUID responsavelId,
      Pageable pageable);

  @Query("SELECT s.modeloId, COUNT(s) FROM SolicitacaoJpaEntity s GROUP BY s.modeloId")
  List<Object[]> countGroupByModeloId();

  long countByStatus(StatusSolicitacao status);

  List<SolicitacaoJpaEntity> findByStatus(StatusSolicitacao status);

  List<SolicitacaoJpaEntity> findByCriadaEmBetween(Instant inicio, Instant fim);

  List<SolicitacaoJpaEntity> findByStatusAndCriadaEmBetween(
      StatusSolicitacao status, Instant inicio, Instant fim);
}
