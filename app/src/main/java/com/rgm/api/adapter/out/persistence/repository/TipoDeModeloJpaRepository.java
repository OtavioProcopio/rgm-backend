package com.rgm.api.adapter.out.persistence.repository;

import com.rgm.api.adapter.out.persistence.entity.TipoDeModeloJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoDeModeloJpaRepository extends JpaRepository<TipoDeModeloJpaEntity, UUID> {

  boolean existsByNomeIgnoreCase(String nome);

  boolean existsByNomeIgnoreCaseAndAtivoTrue(String nome);
}
