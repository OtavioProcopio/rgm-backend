package com.rgm.api.adapter.out.persistence.repository;

import com.rgm.api.adapter.out.persistence.entity.SolicitacaoEvidenciaJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitacaoEvidenciaJpaRepository
    extends JpaRepository<SolicitacaoEvidenciaJpaEntity, UUID> {

  List<SolicitacaoEvidenciaJpaEntity> findBySolicitacaoId(UUID solicitacaoId);

  boolean existsBySolicitacaoIdAndEvidenciaId(UUID solicitacaoId, UUID evidenciaId);

  void deleteBySolicitacaoId(UUID solicitacaoId);
}
