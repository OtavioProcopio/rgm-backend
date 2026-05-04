package com.rgm.api.adapter.out.persistence;

import com.rgm.api.adapter.out.persistence.entity.EventoModeloEvidenciaJpaEntity;
import com.rgm.api.adapter.out.persistence.repository.EventoModeloEvidenciaJpaRepository;
import com.rgm.api.core.domain.model.entities.EventoModeloEvidencia;
import com.rgm.api.core.domain.ports.repositories.EventoModeloEvidenciaRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class EventoModeloEvidenciaRepositoryAdapter implements EventoModeloEvidenciaRepository {

  private final EventoModeloEvidenciaJpaRepository jpa;

  public EventoModeloEvidenciaRepositoryAdapter(final EventoModeloEvidenciaJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public EventoModeloEvidencia save(final EventoModeloEvidencia eme) {
    final EventoModeloEvidenciaJpaEntity entity = new EventoModeloEvidenciaJpaEntity();
    entity.setId(UUID.randomUUID());
    entity.setEventoModeloId(eme.getEventoModeloId());
    entity.setEvidenciaId(eme.getEvidenciaId());
    final EventoModeloEvidenciaJpaEntity saved = jpa.save(entity);
    return new EventoModeloEvidencia(saved.getEventoModeloId(), saved.getEvidenciaId());
  }

  @Override
  public List<EventoModeloEvidencia> findByEventoModeloId(final UUID eventoModeloId) {
    return jpa.findByEventoModeloId(eventoModeloId).stream()
        .map(e -> new EventoModeloEvidencia(e.getEventoModeloId(), e.getEvidenciaId()))
        .toList();
  }

  @Override
  public boolean existsByEvidenciaIdAndEventoModeloModeloId(
      final UUID evidenciaId, final UUID modeloId) {
    return jpa.existsByEvidenciaIdAndEventoModeloModeloId(evidenciaId, modeloId);
  }
}
