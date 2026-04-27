package com.rgm.api.adapter.out.persistence.repository;

import com.rgm.api.adapter.out.persistence.entity.EventoModeloEvidenciaJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventoModeloEvidenciaJpaRepository
    extends JpaRepository<EventoModeloEvidenciaJpaEntity, UUID> {

  List<EventoModeloEvidenciaJpaEntity> findByEventoModeloId(UUID eventoModeloId);

  @Query(
      "SELECT COUNT(eme) > 0 FROM EventoModeloEvidenciaJpaEntity eme "
          + "JOIN EventoModeloJpaEntity em ON eme.eventoModeloId = em.id "
          + "WHERE eme.evidenciaId = :evidenciaId AND em.modeloId = :modeloId")
  boolean existsByEvidenciaIdAndEventoModeloModeloId(
      @Param("evidenciaId") UUID evidenciaId, @Param("modeloId") UUID modeloId);
}
