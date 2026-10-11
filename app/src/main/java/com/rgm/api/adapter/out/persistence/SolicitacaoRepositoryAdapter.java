package com.rgm.api.adapter.out.persistence;

import com.rgm.api.adapter.out.persistence.mapper.SolicitacaoMapper;
import com.rgm.api.adapter.out.persistence.repository.SolicitacaoJpaRepository;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.enums.OrdenacaoMetricaModelo;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.GranularidadeSerie;
import com.rgm.api.core.domain.ports.repositories.LinhaDeSerie;
import com.rgm.api.core.domain.ports.repositories.MetricaModeloRow;
import com.rgm.api.core.domain.ports.repositories.OrdenacaoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import com.rgm.api.core.domain.ports.repositories.PainelAgregado;
import com.rgm.api.core.domain.ports.repositories.ResumoSolicitacoesModelo;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
    final var ordem =
        Sort.by(Sort.Direction.ASC, "criadaEm").and(Sort.by(Sort.Direction.ASC, "id"));
    final var pageable = PageRequest.of(page, size, ordem);
    final var result = jpa.findAll(pageable);
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
      final Boolean atrasada,
      final boolean emAberto,
      final UUID visivelParaUsuarioId,
      final String busca,
      final OrdenacaoSolicitacao ordenacao,
      final int page,
      final int size) {
    // A ordenacao vai dentro da consulta (criterio e sentido vem de enum, nunca do cliente).
    final var pageable = PageRequest.of(page, size);
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
            atrasada,
            emAberto,
            visivelParaUsuarioId,
            busca == null ? null : TextoDeBusca.escaparCuringas(busca),
            ordenacao.criterio().name(),
            ordenacao.ascendente(),
            pageable);
    return new PageResult<>(
        result.getContent().stream().map(SolicitacaoMapper::toDomain).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Override
  public Map<String, Long> countEmAbertoPorTipo() {
    final Map<String, Long> result = new HashMap<>();
    for (final Object[] row : jpa.countEmAbertoPorTipo()) {
      result.put(((Enum<?>) row[0]).name(), (Long) row[1]);
    }
    return result;
  }

  @Override
  public Map<String, Long> countEmAbertoPorPrioridade() {
    final Map<String, Long> result = new HashMap<>();
    for (final Object[] row : jpa.countEmAbertoPorPrioridade()) {
      result.put(row[0] == null ? "SEM_PRIORIDADE" : ((Enum<?>) row[0]).name(), (Long) row[1]);
    }
    return result;
  }

  @Override
  public Map<UUID, Long> countGroupByModeloId() {
    final Map<UUID, Long> result = new HashMap<>();
    for (final Object[] row : jpa.countGroupByModeloId()) {
      // modelo_id e nulo para solicitacoes CRIACAO ainda nao concluidas — nao ha
      // modelo a que atribuir essa contagem, entao a linha e ignorada aqui (um Map
      // com chave nula quebraria a serializacao JSON da resposta).
      if (row[0] == null) {
        continue;
      }
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
  public List<LinhaDeSerie> agregarSerie(
      final Instant inicio,
      final Instant fim,
      final UUID modeloId,
      final GranularidadeSerie granularidade,
      final ZoneId fuso) {
    final String unidade = unidadeDe(granularidade);
    return jpa.agregarSerie(inicio, fim, modeloId, unidade, fuso.getId()).stream()
        .map(
            row ->
                new LinhaDeSerie(
                    LocalDate.parse(row[0].toString()),
                    ((Number) row[1]).longValue(),
                    ((Number) row[2]).longValue(),
                    ((Number) row[3]).longValue(),
                    ((Number) row[4]).longValue(),
                    row[5] != null ? ((Number) row[5]).doubleValue() : null))
        .toList();
  }

  @Override
  public PainelAgregado agregarPainel(
      final Instant inicio,
      final Instant fim,
      final UUID modeloId,
      final GranularidadeSerie granularidade,
      final ZoneId fuso,
      final Instant agora) {
    final String unidade = unidadeDe(granularidade);
    final Object[] totais = jpa.painelTotais(inicio, fim, modeloId).get(0);
    final List<PainelAgregado.Ponto> serie =
        jpa
            .painelSerie(
                inicio.atZone(fuso).toLocalDate(),
                fim.atZone(fuso).toLocalDate(),
                modeloId,
                unidade,
                fuso.getId(),
                agora)
            .stream()
            .map(SolicitacaoRepositoryAdapter::paraPonto)
            .toList();
    return new PainelAgregado(
        new PainelAgregado.Totais(
            ((Number) totais[0]).longValue(),
            ((Number) totais[1]).longValue(),
            ((Number) totais[2]).longValue(),
            totais[3] != null ? ((Number) totais[3]).doubleValue() : null),
        serie);
  }

  private static String unidadeDe(final GranularidadeSerie granularidade) {
    return granularidade == GranularidadeSerie.SEMANA ? "week" : "day";
  }

  private static PainelAgregado.Ponto paraPonto(final Object[] row) {
    return new PainelAgregado.Ponto(
        LocalDate.parse(row[0].toString()),
        ((Number) row[1]).longValue(),
        ((Number) row[2]).longValue(),
        ((Number) row[3]).longValue(),
        ((Number) row[4]).longValue(),
        ((Number) row[5]).longValue());
  }

  @Override
  public PainelAgregado.Agora situacaoAgora(final UUID modeloId, final Instant agora) {
    final Object[] row = jpa.painelAgora(modeloId, agora).get(0);
    return new PainelAgregado.Agora(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
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

  @Override
  public ResumoSolicitacoesModelo resumirPorModelo(final UUID modeloId) {
    final Object[] row = jpa.resumirPorModelo(modeloId).get(0);
    return ResumoSolicitacoesModelo.of(
        ((Number) row[0]).longValue(),
        ((Number) row[1]).longValue(),
        ((Number) row[2]).longValue(),
        ((Number) row[3]).longValue(),
        row[6] != null ? ((Number) row[6]).doubleValue() : null,
        ((Number) row[7]).longValue(),
        (Instant) row[4],
        (Instant) row[5]);
  }
}
