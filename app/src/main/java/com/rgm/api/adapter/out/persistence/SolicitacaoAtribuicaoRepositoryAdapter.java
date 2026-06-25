package com.rgm.api.adapter.out.persistence;

import com.rgm.api.adapter.out.persistence.mapper.SolicitacaoAtribuicaoMapper;
import com.rgm.api.adapter.out.persistence.repository.SolicitacaoAtribuicaoJpaRepository;
import com.rgm.api.core.domain.model.entities.SolicitacaoAtribuicao;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class SolicitacaoAtribuicaoRepositoryAdapter implements SolicitacaoAtribuicaoRepository {

  private final SolicitacaoAtribuicaoJpaRepository jpa;

  public SolicitacaoAtribuicaoRepositoryAdapter(final SolicitacaoAtribuicaoJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public SolicitacaoAtribuicao save(final SolicitacaoAtribuicao atribuicao) {
    return SolicitacaoAtribuicaoMapper.toDomain(
        jpa.save(SolicitacaoAtribuicaoMapper.toJpa(atribuicao)));
  }

  @Override
  public List<SolicitacaoAtribuicao> findBySolicitacaoId(final UUID solicitacaoId) {
    return jpa.findBySolicitacaoId(solicitacaoId).stream()
        .map(SolicitacaoAtribuicaoMapper::toDomain)
        .toList();
  }

  @Override
  public boolean existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(
      final UUID solicitacaoId, final UUID usuarioId) {
    return jpa.existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(solicitacaoId, usuarioId);
  }

  @Override
  public boolean existsByUsuarioIdAndRemovidoEmIsNull(final UUID usuarioId) {
    return jpa.existsByUsuarioIdAndRemovidoEmIsNull(usuarioId);
  }

  @Override
  @Transactional
  public void deleteBySolicitacaoId(final UUID solicitacaoId) {
    jpa.deleteBySolicitacaoId(solicitacaoId);
  }
}
