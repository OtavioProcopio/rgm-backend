package com.rgm.api.adapter.out.persistence;

import com.rgm.api.adapter.out.persistence.mapper.EvidenciaMapper;
import com.rgm.api.adapter.out.persistence.repository.EvidenciaJpaRepository;
import com.rgm.api.core.domain.model.aggregates.Evidencia;
import com.rgm.api.core.domain.ports.repositories.EvidenciaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class EvidenciaRepositoryAdapter implements EvidenciaRepository {

  private final EvidenciaJpaRepository jpa;

  public EvidenciaRepositoryAdapter(final EvidenciaJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<Evidencia> findById(final UUID id) {
    return jpa.findById(id).map(EvidenciaMapper::toDomain);
  }

  @Override
  public Evidencia save(final Evidencia evidencia) {
    return EvidenciaMapper.toDomain(jpa.save(EvidenciaMapper.toJpa(evidencia)));
  }
}
