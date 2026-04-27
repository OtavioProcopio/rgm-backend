package com.rgm.api.adapter.out.persistence.repository;

import com.rgm.api.adapter.out.persistence.entity.MaquinaJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaquinaJpaRepository extends JpaRepository<MaquinaJpaEntity, UUID> {}
