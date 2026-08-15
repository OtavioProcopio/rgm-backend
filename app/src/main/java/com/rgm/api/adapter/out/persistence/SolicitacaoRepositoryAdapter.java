package com.rgm.api.adapter.out.persistence;

import com.rgm.api.adapter.out.persistence.mapper.SolicitacaoMapper;
import com.rgm.api.adapter.out.persistence.repository.SolicitacaoJpaRepository;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.enums.OrdenacaoMetricaModelo;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.MetricaModeloRow;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    final var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "criadaEm"));
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
    final var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "criadaEm"));
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
      final StatusSolicitacao status,
      final UUID modeloId,
      final TipoSolicitacao tipo,
      final PrioridadeSolicitacao prioridade,
      final Instant criadaEmInicio,
      final Instant criadaEmFim,
      final Instant concluidaEmInicio,
      final Instant concluidaEmFim,
      final UUID abertaPorUsuarioId,
      final UUID responsavelId,
      final String maquina,
      final int page,
      final int size) {
    final var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "criada_em"));
    final var result =
        jpa.findByFilters(
            status != null ? status.name() : null,
            modeloId,
            tipo != null ? tipo.name() : null,
            prioridade != null ? prioridade.name() : null,
            criadaEmInicio,
            criadaEmFim,
            concluidaEmInicio,
            concluidaEmFim,
            abertaPorUsuarioId,
            responsavelId,
            maquina,
            pageable);
    return new PageResult<>(
        result.getContent().stream().map(SolicitacaoMapper::toDomain).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Override
  public Map<UUID, Long> countGroupByModeloId() {
    final Map<UUID, Long> result = new HashMap<>();
    for (final Object[] row : jpa.countGroupByModeloId()) {
      result.put((UUID) row[0], (Long) row[1]);
    }
    return result;
  }

  @Override
  public long count() {
    return jpa.count();
  }

  @Override
  public long countByStatus(final StatusSolicitacao status) {
    return jpa.countByStatus(status);
  }

  @Override
  public List<Solicitacao> findByStatus(final StatusSolicitacao status) {
    return jpa.findByStatus(status).stream().map(SolicitacaoMapper::toDomain).toList();
  }

  @Override
  public List<Solicitacao> findByCriadaEmBetween(final Instant inicio, final Instant fim) {
    return jpa.findByCriadaEmBetween(inicio, fim).stream()
        .map(SolicitacaoMapper::toDomain)
        .toList();
  }

  @Override
  public List<Solicitacao> findByStatusAndCriadaEmBetween(
      final StatusSolicitacao status, final Instant inicio, final Instant fim) {
    return jpa.findByStatusAndCriadaEmBetween(status, inicio, fim).stream()
        .map(SolicitacaoMapper::toDomain)
        .toList();
  }

  @Override
  public long getTempoMedioResolucaoSegundos() {
    return (long) jpa.getTempoMedioResolucaoSegundos();
  }

  @Override
  public PageResult<MetricaModeloRow> findMetricasPorModelo(
      final OrdenacaoMetricaModelo sort, final boolean ascendente, final int page, final int size) {
    final String coluna =
        sort == OrdenacaoMetricaModelo.INTERVALO ? "intervalo_medio" : "tempo_medio_resolucao";
    final var direcao = ascendente ? Sort.Direction.ASC : Sort.Direction.DESC;
    final var pageable = PageRequest.of(page, size, Sort.by(direcao, coluna));

    final var result = jpa.findMetricasPorModelo(pageable);
    final List<MetricaModeloRow> content =
        result.getContent().stream()
            .map(
                row ->
                    new MetricaModeloRow(
                        (UUID) row[0],
                        (String) row[1],
                        ((Number) row[2]).doubleValue(),
                        row[3] != null ? ((Number) row[3]).doubleValue() : null))
            .toList();

    return new PageResult<>(
        content,
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }
}
