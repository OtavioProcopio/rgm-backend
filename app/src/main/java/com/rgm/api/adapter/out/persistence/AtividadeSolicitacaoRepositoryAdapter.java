package com.rgm.api.adapter.out.persistence;

import com.rgm.api.adapter.out.persistence.mapper.AtividadeSolicitacaoMapper;
import com.rgm.api.adapter.out.persistence.repository.AtividadeSolicitacaoJpaRepository;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class AtividadeSolicitacaoRepositoryAdapter implements AtividadeSolicitacaoRepository {

  private final AtividadeSolicitacaoJpaRepository jpa;

  public AtividadeSolicitacaoRepositoryAdapter(final AtividadeSolicitacaoJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public AtividadeSolicitacao save(final AtividadeSolicitacao atividade) {
    return AtividadeSolicitacaoMapper.toDomain(
        jpa.save(AtividadeSolicitacaoMapper.toJpa(atividade)));
  }

  @Override
  public List<AtividadeSolicitacao> findBySolicitacaoId(final UUID solicitacaoId) {
    return jpa.findBySolicitacaoId(solicitacaoId).stream()
        .map(AtividadeSolicitacaoMapper::toDomain)
        .toList();
  }

  @Override
  @Transactional
  public void deleteBySolicitacaoId(final UUID solicitacaoId) {
    jpa.deleteBySolicitacaoId(solicitacaoId);
  }
}
