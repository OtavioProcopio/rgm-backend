package com.rgm.api.adapter.out.persistence;

import com.rgm.api.adapter.out.persistence.entity.SolicitacaoEvidenciaJpaEntity;
import com.rgm.api.adapter.out.persistence.repository.SolicitacaoEvidenciaJpaRepository;
import com.rgm.api.core.domain.model.entities.SolicitacaoEvidencia;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoEvidenciaRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class SolicitacaoEvidenciaRepositoryAdapter implements SolicitacaoEvidenciaRepository {

  private final SolicitacaoEvidenciaJpaRepository jpa;

  public SolicitacaoEvidenciaRepositoryAdapter(final SolicitacaoEvidenciaJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public SolicitacaoEvidencia save(final SolicitacaoEvidencia se) {
    final SolicitacaoEvidenciaJpaEntity entity = new SolicitacaoEvidenciaJpaEntity();
    entity.setId(UUID.randomUUID());
    entity.setSolicitacaoId(se.getSolicitacaoId());
    entity.setEvidenciaId(se.getEvidenciaId());
    final SolicitacaoEvidenciaJpaEntity saved = jpa.save(entity);
    return new SolicitacaoEvidencia(saved.getSolicitacaoId(), saved.getEvidenciaId());
  }

  @Override
  public List<SolicitacaoEvidencia> findBySolicitacaoId(final UUID solicitacaoId) {
    return jpa.findBySolicitacaoId(solicitacaoId).stream()
        .map(e -> new SolicitacaoEvidencia(e.getSolicitacaoId(), e.getEvidenciaId()))
        .toList();
  }

  @Override
  public boolean existsBySolicitacaoIdAndEvidenciaId(
      final UUID solicitacaoId, final UUID evidenciaId) {
    return jpa.existsBySolicitacaoIdAndEvidenciaId(solicitacaoId, evidenciaId);
  }

  @Override
  @Transactional
  public void deleteBySolicitacaoId(final UUID solicitacaoId) {
    jpa.deleteBySolicitacaoId(solicitacaoId);
  }
}
