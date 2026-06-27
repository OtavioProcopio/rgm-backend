package com.rgm.api.adapter.out.persistence.repository;

import com.rgm.api.adapter.out.persistence.entity.EventoModeloJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoModeloJpaRepository extends JpaRepository<EventoModeloJpaEntity, UUID> {

  List<EventoModeloJpaEntity> findByModeloId(UUID modeloId);

  boolean existsByExecutadoPorUsuarioId(UUID usuarioId);
}
