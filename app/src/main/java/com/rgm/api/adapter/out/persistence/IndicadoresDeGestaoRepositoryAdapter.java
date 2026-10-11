package com.rgm.api.adapter.out.persistence;

import com.rgm.api.adapter.out.persistence.repository.IndicadoresDeGestaoJpaRepository;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.ports.repositories.IndicadoresDeGestaoRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class IndicadoresDeGestaoRepositoryAdapter implements IndicadoresDeGestaoRepository {

  private final IndicadoresDeGestaoJpaRepository jpa;

  public IndicadoresDeGestaoRepositoryAdapter(final IndicadoresDeGestaoJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public List<PrazoPorPrioridade> prazoPorPrioridade(final Instant inicio, final Instant fim) {
    return jpa.prazoPorPrioridade(inicio, fim).stream()
        .map(
            l ->
                new PrazoPorPrioridade(
                    PrioridadeSolicitacao.valueOf((String) l[0]), longo(l[1]), longo(l[2])))
        .toList();
  }

  @Override
  public long concluidasSemPrioridade(final Instant inicio, final Instant fim) {
    return jpa.concluidasSemPrioridade(inicio, fim);
  }

  @Override
  public ResolucaoNoPeriodo resolucao(final Instant inicio, final Instant fim) {
    final Object[] l = jpa.resolucao(inicio, fim).get(0);
    return new ResolucaoNoPeriodo(longo(l[0]), duplo(l[1]), duplo(l[2]));
  }

  @Override
  public BacklogAgora backlog(final Instant agora) {
    final Object[] l = jpa.backlog(agora).get(0);
    return new BacklogAgora(longo(l[0]), longo(l[1]), longo(l[2]), longo(l[3]), longo(l[4]));
  }

  @Override
  public List<TempoDeEtapa> tempoPorEtapa(final Instant inicio, final Instant fim) {
    return jpa.tempoPorEtapa(inicio, fim).stream()
        .map(l -> new TempoDeEtapa((String) l[0], longo(l[1]), duplo(l[2]), duplo(l[3])))
        .toList();
  }

  @Override
  public Devolucoes devolucoes(final Instant inicio, final Instant fim) {
    final Object[] l = jpa.devolucoes(inicio, fim).get(0);
    return new Devolucoes(longo(l[0]), longo(l[1]));
  }

  @Override
  public List<DesempenhoDaMaquina> porMaquina(
      final Instant inicio, final Instant fim, final Instant agora) {
    return jpa.porMaquina(inicio, fim, agora).stream()
        .map(l -> new DesempenhoDaMaquina((String) l[0], longo(l[1]), longo(l[2]), duplo(l[3])))
        .toList();
  }

  @Override
  public List<ModeloReincidente> reincidentes(
      final Instant inicio, final Instant fim, final int minimo) {
    return jpa.reincidentes(inicio, fim, minimo).stream()
        .map(
            l ->
                new ModeloReincidente(
                    (UUID) l[0],
                    (String) l[1],
                    ((Number) l[2]).intValue(),
                    (String) l[3],
                    longo(l[4])))
        .toList();
  }

  @Override
  public long abertasComResponsavel() {
    return jpa.abertasComResponsavel();
  }

  @Override
  public List<CargaDoResponsavel> cargaPorResponsavel() {
    return jpa.cargaPorResponsavel().stream()
        .map(l -> new CargaDoResponsavel((UUID) l[0], (String) l[1], longo(l[2])))
        .toList();
  }

  private static long longo(final Object valor) {
    return ((Number) valor).longValue();
  }

  private static Double duplo(final Object valor) {
    return valor == null ? null : ((Number) valor).doubleValue();
  }
}
