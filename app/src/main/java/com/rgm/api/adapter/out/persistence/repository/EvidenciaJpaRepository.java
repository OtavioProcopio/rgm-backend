package com.rgm.api.adapter.out.persistence.repository;

import com.rgm.api.adapter.out.persistence.entity.EvidenciaJpaEntity;
import com.rgm.api.core.domain.model.enums.TipoEvidencia;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EvidenciaJpaRepository extends JpaRepository<EvidenciaJpaEntity, UUID> {

  @Query(
      "SELECT COUNT(e) > 0 FROM EvidenciaJpaEntity e, SolicitacaoEvidenciaJpaEntity se "
          + "WHERE se.solicitacaoId = :solicitacaoId AND se.evidenciaId = e.id AND e.tipo = :tipo")
  boolean existsBySolicitacaoIdAndTipo(
      @Param("solicitacaoId") UUID solicitacaoId, @Param("tipo") TipoEvidencia tipo);
}
