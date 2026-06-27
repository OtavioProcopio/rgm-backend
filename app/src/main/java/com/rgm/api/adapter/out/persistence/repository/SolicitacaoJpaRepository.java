package com.rgm.api.adapter.out.persistence.repository;

import com.rgm.api.adapter.out.persistence.entity.SolicitacaoJpaEntity;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
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
      "SELECT s FROM SolicitacaoJpaEntity s WHERE "
          + "(:status IS NULL OR s.status = :status) AND "
          + "(:modeloId IS NULL OR s.modeloId = :modeloId) AND "
          + "(:tipo IS NULL OR s.tipo = :tipo) AND "
          + "(:prioridade IS NULL OR s.prioridade = :prioridade) AND "
          + "(:criadaEmInicio IS NULL OR s.criadaEm >= :criadaEmInicio) AND "
          + "(:criadaEmFim IS NULL OR s.criadaEm <= :criadaEmFim) AND "
          + "(:abertaPorUsuarioId IS NULL OR s.abertaPorUsuarioId = :abertaPorUsuarioId) AND "
          + "(:responsavelId IS NULL OR EXISTS (SELECT a FROM SolicitacaoAtribuicaoJpaEntity a WHERE a.solicitacaoId = s.id AND a.usuarioId = :responsavelId AND a.removidoEm IS NULL))")
  Page<SolicitacaoJpaEntity> findByFilters(
      StatusSolicitacao status,
      UUID modeloId,
      TipoSolicitacao tipo,
      PrioridadeSolicitacao prioridade,
      Instant criadaEmInicio,
      Instant criadaEmFim,
      UUID abertaPorUsuarioId,
      UUID responsavelId,
      Pageable pageable);

  @Query("SELECT s.modeloId, COUNT(s) FROM SolicitacaoJpaEntity s GROUP BY s.modeloId")
  List<Object[]> countGroupByModeloId();

  long countByStatus(StatusSolicitacao status);

  List<SolicitacaoJpaEntity> findByStatus(StatusSolicitacao status);
}
