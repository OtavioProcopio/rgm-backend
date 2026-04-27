package com.rgm.api.adapter.out.persistence.repository;

import com.rgm.api.adapter.out.persistence.entity.EvidenciaJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenciaJpaRepository extends JpaRepository<EvidenciaJpaEntity, UUID> {}
