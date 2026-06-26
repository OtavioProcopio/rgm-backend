package com.rgm.api.adapter.out.persistence.repository;

import com.rgm.api.adapter.out.persistence.entity.AtividadeSolicitacaoJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtividadeSolicitacaoJpaRepository
    extends JpaRepository<AtividadeSolicitacaoJpaEntity, UUID> {

  List<AtividadeSolicitacaoJpaEntity> findBySolicitacaoId(UUID solicitacaoId);

  boolean existsByAutorUsuarioId(UUID autorUsuarioId);

  void deleteBySolicitacaoId(UUID solicitacaoId);
}
