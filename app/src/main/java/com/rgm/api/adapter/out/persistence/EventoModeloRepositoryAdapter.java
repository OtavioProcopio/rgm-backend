package com.rgm.api.adapter.out.persistence;

import com.rgm.api.adapter.out.persistence.mapper.EventoModeloMapper;
import com.rgm.api.adapter.out.persistence.repository.EventoModeloJpaRepository;
import com.rgm.api.core.domain.model.aggregates.EventoModelo;
import com.rgm.api.core.domain.ports.repositories.EventoModeloRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class EventoModeloRepositoryAdapter implements EventoModeloRepository {

  private final EventoModeloJpaRepository jpa;

  public EventoModeloRepositoryAdapter(final EventoModeloJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public EventoModelo save(final EventoModelo eventoModelo) {
    return EventoModeloMapper.toDomain(jpa.save(EventoModeloMapper.toJpa(eventoModelo)));
  }

  @Override
  public List<EventoModelo> findByModeloId(final UUID modeloId) {
    return jpa.findByModeloId(modeloId).stream().map(EventoModeloMapper::toDomain).toList();
  }

  @Override
  public boolean existsByExecutadoPorUsuarioId(final UUID usuarioId) {
    return jpa.existsByExecutadoPorUsuarioId(usuarioId);
  }
}
