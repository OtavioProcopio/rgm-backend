package com.rgm.api.adapter.out.persistence.repository;

import com.rgm.api.adapter.out.persistence.entity.UsuarioJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioJpaEntity, UUID> {

  Optional<UsuarioJpaEntity> findByEmail(String email);

  List<UsuarioJpaEntity> findAllByIdIn(List<UUID> ids);

  boolean existsByEmail(String email);

  @Query(
      "SELECT u FROM UsuarioJpaEntity u WHERE "
          + "(:perfil IS NULL OR u.perfil = :perfil) AND "
          + "(:ativo IS NULL OR u.ativo = :ativo)")
  Page<UsuarioJpaEntity> findByFilters(String perfil, Boolean ativo, Pageable pageable);
}
