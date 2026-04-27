package com.rgm.api.adapter.out.persistence.repository;

import com.rgm.api.adapter.out.persistence.entity.SolicitacaoAtribuicaoJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitacaoAtribuicaoJpaRepository
    extends JpaRepository<SolicitacaoAtribuicaoJpaEntity, UUID> {

  List<SolicitacaoAtribuicaoJpaEntity> findBySolicitacaoId(UUID solicitacaoId);

  boolean existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(UUID solicitacaoId, UUID usuarioId);

  void deleteBySolicitacaoId(UUID solicitacaoId);
}
