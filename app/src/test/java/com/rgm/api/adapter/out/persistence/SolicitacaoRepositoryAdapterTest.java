package com.rgm.api.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rgm.api.adapter.out.persistence.entity.SolicitacaoJpaEntity;
import com.rgm.api.adapter.out.persistence.repository.SolicitacaoJpaRepository;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class SolicitacaoRepositoryAdapterTest {

  private SolicitacaoJpaRepository jpa;
  private SolicitacaoRepositoryAdapter adapter;

  @BeforeEach
  void setUp() {
    jpa = mock(SolicitacaoJpaRepository.class);
    adapter = new SolicitacaoRepositoryAdapter(jpa);
  }

  private SolicitacaoJpaEntity criarEntity() {
    final SolicitacaoJpaEntity e = new SolicitacaoJpaEntity();
    e.setId(UUID.randomUUID());
    e.setTitulo("Trocar peça");
    e.setDescricao("Desc");
    e.setTipo(TipoSolicitacao.REPARO);
    e.setStatus(StatusSolicitacao.A_FAZER);
    e.setPrioridade(PrioridadeSolicitacao.MEDIA);
    e.setModeloId(UUID.randomUUID());
    e.setAbertaPorUsuarioId(UUID.randomUUID());
    e.setCriadaEm(Instant.now());
    e.setAtualizadaEm(Instant.now());
    return e;
  }

  private Solicitacao criarDomain() {
    return new Solicitacao(
        UUID.randomUUID(),
        "Trocar peça",
        "Desc",
        TipoSolicitacao.REPARO,
        StatusSolicitacao.A_FAZER,
        PrioridadeSolicitacao.MEDIA,
        UUID.randomUUID(),
        UUID.randomUUID(),
        null,
        Instant.now(),
        Instant.now(),
        null,
        null);
  }

  @Test
  void findById_quandoExistir_retornaSolicitacao() {
    final UUID id = UUID.randomUUID();
    final SolicitacaoJpaEntity e = criarEntity();
    e.setId(id);
    when(jpa.findById(id)).thenReturn(Optional.of(e));

    final Optional<Solicitacao> result = adapter.findById(id);

    assertTrue(result.isPresent());
    assertEquals("Trocar peça", result.get().getTitulo());
  }

  @Test
  void findById_quandoNaoExistir_retornaVazio() {
    final UUID id = UUID.randomUUID();
    when(jpa.findById(id)).thenReturn(Optional.empty());

    assertFalse(adapter.findById(id).isPresent());
  }

  @Test
  void save_persisteERetorna() {
    final Solicitacao domain = criarDomain();
    final SolicitacaoJpaEntity e = criarEntity();
    when(jpa.save(any(SolicitacaoJpaEntity.class))).thenReturn(e);

    final Solicitacao result = adapter.save(domain);

    assertNotNull(result);
    verify(jpa).save(any(SolicitacaoJpaEntity.class));
  }

  @Test
  void deleteById_delegaAoJpa() {
    final UUID id = UUID.randomUUID();
    adapter.deleteById(id);
    verify(jpa).deleteById(id);
  }

  @Test
  void existsByModeloIdAndStatusIn_delegaAoJpa() {
    final UUID modeloId = UUID.randomUUID();
    when(jpa.existsByModeloIdAndStatusIn(modeloId, List.of(StatusSolicitacao.A_FAZER)))
        .thenReturn(true);

    assertTrue(adapter.existsByModeloIdAndStatusIn(modeloId, List.of(StatusSolicitacao.A_FAZER)));
  }

  @Test
  void existsByModeloId_delegaAoJpa() {
    final UUID id = UUID.randomUUID();
    when(jpa.existsByModeloId(id)).thenReturn(false);

    assertFalse(adapter.existsByModeloId(id));
  }

  @Test
  void existsByAbertaPorUsuarioId_delegaAoJpa() {
    final UUID id = UUID.randomUUID();
    when(jpa.existsByAbertaPorUsuarioId(id)).thenReturn(true);

    assertTrue(adapter.existsByAbertaPorUsuarioId(id));
  }

  @Test
  void findByModeloId_retornaLista() {
    final UUID modeloId = UUID.randomUUID();
    final SolicitacaoJpaEntity e = criarEntity();
    e.setModeloId(modeloId);
    when(jpa.findByModeloId(modeloId)).thenReturn(List.of(e));

    final List<Solicitacao> result = adapter.findByModeloId(modeloId);

    assertEquals(1, result.size());
  }

  @Test
  void findAll_retornaPaginado() {
    final SolicitacaoJpaEntity e = criarEntity();
    final Page<SolicitacaoJpaEntity> page = new PageImpl<>(List.of(e), PageRequest.of(0, 10), 1);
    when(jpa.findAll(any(PageRequest.class))).thenReturn(page);

    final PageResult<Solicitacao> result = adapter.findAll(0, 10);

    assertEquals(1, result.content().size());
    assertEquals(1L, result.totalElements());
  }

  @Test
  void findByStatus_paginado_retornaPaginado() {
    final SolicitacaoJpaEntity e = criarEntity();
    final Page<SolicitacaoJpaEntity> page = new PageImpl<>(List.of(e), PageRequest.of(0, 10), 1);
    when(jpa.findByStatus(any(StatusSolicitacao.class), any(PageRequest.class))).thenReturn(page);

    final PageResult<Solicitacao> result = adapter.findByStatus(StatusSolicitacao.A_FAZER, 0, 10);

    assertEquals(1, result.content().size());
  }

  @Test
  void findByStatus_lista_retornaLista() {
    final SolicitacaoJpaEntity e = criarEntity();
    when(jpa.findByStatus(StatusSolicitacao.A_FAZER)).thenReturn(List.of(e));

    final List<Solicitacao> result = adapter.findByStatus(StatusSolicitacao.A_FAZER);

    assertEquals(1, result.size());
  }

  @Test
  void findByFilters_retornaPaginado() {
    final SolicitacaoJpaEntity e = criarEntity();
    final Page<SolicitacaoJpaEntity> page = new PageImpl<>(List.of(e), PageRequest.of(0, 10), 1);
    when(jpa.findByFilters(any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(page);

    final PageResult<Solicitacao> result =
        adapter.findByFilters(
            StatusSolicitacao.A_FAZER,
            null,
            TipoSolicitacao.REPARO,
            null,
            null,
            null,
            null,
            null,
            0,
            10);

    assertEquals(1, result.content().size());
  }

  @Test
  void findByFilters_comNulos_retornaPaginado() {
    final Page<SolicitacaoJpaEntity> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
    when(jpa.findByFilters(any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(page);

    final PageResult<Solicitacao> result =
        adapter.findByFilters(null, null, null, null, null, null, null, null, 0, 10);

    assertEquals(0, result.content().size());
  }

  @Test
  void countGroupByModeloId_retornaMapa() {
    final UUID modeloId = UUID.randomUUID();
    final Object[] row = {modeloId, 3L};
    final List<Object[]> rows = new ArrayList<>();
    rows.add(row);
    when(jpa.countGroupByModeloId()).thenReturn(rows);

    final Map<UUID, Long> result = adapter.countGroupByModeloId();

    assertEquals(1, result.size());
    assertEquals(3L, result.get(modeloId));
  }

  @Test
  void count_delegaAoJpa() {
    when(jpa.count()).thenReturn(42L);

    assertEquals(42L, adapter.count());
  }

  @Test
  void countByStatus_delegaAoJpa() {
    when(jpa.countByStatus(StatusSolicitacao.A_FAZER)).thenReturn(5L);

    assertEquals(5L, adapter.countByStatus(StatusSolicitacao.A_FAZER));
  }

  @Test
  void findByCriadaEmBetween_retornaLista() {
    final Instant inicio = Instant.now().minusSeconds(3600);
    final Instant fim = Instant.now();
    final SolicitacaoJpaEntity e = criarEntity();
    when(jpa.findByCriadaEmBetween(inicio, fim)).thenReturn(List.of(e));

    final List<Solicitacao> result = adapter.findByCriadaEmBetween(inicio, fim);

    assertEquals(1, result.size());
  }

  @Test
  void findByStatusAndCriadaEmBetween_retornaLista() {
    final Instant inicio = Instant.now().minusSeconds(3600);
    final Instant fim = Instant.now();
    final SolicitacaoJpaEntity e = criarEntity();
    when(jpa.findByStatusAndCriadaEmBetween(StatusSolicitacao.A_FAZER, inicio, fim))
        .thenReturn(List.of(e));

    final List<Solicitacao> result =
        adapter.findByStatusAndCriadaEmBetween(StatusSolicitacao.A_FAZER, inicio, fim);

    assertEquals(1, result.size());
  }
}
