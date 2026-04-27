package com.rgm.api.adapter.out.persistence.repository;

import com.rgm.api.adapter.out.persistence.entity.ModeloJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModeloJpaRepository extends JpaRepository<ModeloJpaEntity, UUID> {

  int countByMaquinaIdAndCodigo(UUID maquinaId, String codigo);
}
