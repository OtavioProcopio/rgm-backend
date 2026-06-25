package com.rgm.api.adapter.out.persistence;

import com.rgm.api.adapter.out.persistence.mapper.SolicitacaoMapper;
import com.rgm.api.adapter.out.persistence.repository.SolicitacaoJpaRepository;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class SolicitacaoRepositoryAdapter implements SolicitacaoRepository {

  private final SolicitacaoJpaRepository jpa;

  public SolicitacaoRepositoryAdapter(final SolicitacaoJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<Solicitacao> findById(final UUID id) {
    return jpa.findById(id).map(SolicitacaoMapper::toDomain);
  }

  @Override
  public Solicitacao save(final Solicitacao solicitacao) {
    return SolicitacaoMapper.toDomain(jpa.save(SolicitacaoMapper.toJpa(solicitacao)));
  }

  @Override
  public void deleteById(final UUID id) {
    jpa.deleteById(id);
  }

  @Override
  public boolean existsByModeloIdAndStatusIn(
      final UUID modeloId, final List<StatusSolicitacao> statuses) {
    return jpa.existsByModeloIdAndStatusIn(modeloId, statuses);
  }

  @Override
  public boolean existsByModeloId(final UUID modeloId) {
    return jpa.existsByModeloId(modeloId);
  }

  @Override
  public boolean existsByAbertaPorUsuarioId(final UUID abertaPorUsuarioId) {
    return jpa.existsByAbertaPorUsuarioId(abertaPorUsuarioId);
  }

  @Override
  public List<Solicitacao> findByModeloId(final UUID modeloId) {
    return jpa.findByModeloId(modeloId).stream().map(SolicitacaoMapper::toDomain).toList();
  }

  @Override
  public PageResult<Solicitacao> findAll(final int page, final int size) {
    final var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "criadaEm"));
    final var result = jpa.findAll(pageable);
    return new PageResult<>(
        result.getContent().stream().map(SolicitacaoMapper::toDomain).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Override
  public PageResult<Solicitacao> findByStatus(
      final StatusSolicitacao status, final int page, final int size) {
    final var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "criadaEm"));
    final var result = jpa.findByStatus(status, pageable);
    return new PageResult<>(
        result.getContent().stream().map(SolicitacaoMapper::toDomain).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Override
  public PageResult<Solicitacao> findByFilters(
      final StatusSolicitacao status, final UUID modeloId, final int page, final int size) {
    final var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "criadaEm"));
    final var result = jpa.findByFilters(status, modeloId, pageable);
    return new PageResult<>(
        result.getContent().stream().map(SolicitacaoMapper::toDomain).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }
}
