package com.rgm.api.adapter.out.persistence.repository;

import com.rgm.api.adapter.out.persistence.entity.ModeloJpaEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ModeloJpaRepository extends JpaRepository<ModeloJpaEntity, UUID> {

  int countByMaquinaIdAndCodigo(UUID maquinaId, String codigo);

  @Query(
      "SELECT m FROM ModeloJpaEntity m WHERE "
          + "(:ativo IS NULL OR m.ativo = :ativo) AND "
          + "(:codigo IS NULL OR LOWER(m.codigo) LIKE LOWER(CONCAT('%', :codigo, '%')))")
  Page<ModeloJpaEntity> findByFilters(Boolean ativo, String codigo, Pageable pageable);
}
