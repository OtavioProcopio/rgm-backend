package com.rgm.api.adapter.out.persistence.repository;

import com.rgm.api.adapter.out.persistence.entity.UsuarioJpaEntity;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
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

  boolean existsByEmailAndIdNot(String email, UUID excludeId);

  @Query(
      "SELECT u FROM UsuarioJpaEntity u WHERE "
          + "(:perfil IS NULL OR u.perfil = :perfil) AND "
          + "(:ativo IS NULL OR u.ativo = :ativo)")
  Page<UsuarioJpaEntity> findByFilters(PerfilUsuario perfil, Boolean ativo, Pageable pageable);

  @Query(
      value =
          "SELECT u.* FROM usuarios u WHERE u.ativo = true AND u.perfil IN (:perfis) AND "
              + "(CAST(:busca AS text) IS NULL OR "
              + "rgm_sem_acento(u.nome) LIKE '%' || rgm_sem_acento(CAST(:busca AS text)) || '%') "
              + "ORDER BY rgm_sem_acento(u.nome), u.id",
      countQuery =
          "SELECT COUNT(*) FROM usuarios u WHERE u.ativo = true AND u.perfil IN (:perfis) AND "
              + "(CAST(:busca AS text) IS NULL OR "
              + "rgm_sem_acento(u.nome) LIKE '%' || rgm_sem_acento(CAST(:busca AS text)) || '%')",
      nativeQuery = true)
  Page<UsuarioJpaEntity> buscarAtribuiveis(
      @org.springframework.data.repository.query.Param("perfis") List<String> perfis,
      @org.springframework.data.repository.query.Param("busca") String busca,
      Pageable pageable);
}
