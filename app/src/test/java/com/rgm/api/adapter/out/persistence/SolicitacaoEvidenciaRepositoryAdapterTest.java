package com.rgm.api.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rgm.api.adapter.out.persistence.entity.SolicitacaoEvidenciaJpaEntity;
import com.rgm.api.adapter.out.persistence.repository.SolicitacaoEvidenciaJpaRepository;
import com.rgm.api.core.domain.model.entities.SolicitacaoEvidencia;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SolicitacaoEvidenciaRepositoryAdapterTest {

  private SolicitacaoEvidenciaJpaRepository jpa;
  private SolicitacaoEvidenciaRepositoryAdapter adapter;

  @BeforeEach
  void setUp() {
    jpa = mock(SolicitacaoEvidenciaJpaRepository.class);
    adapter = new SolicitacaoEvidenciaRepositoryAdapter(jpa);
  }

  private SolicitacaoEvidenciaJpaEntity criarEntity(
      final UUID solicitacaoId, final UUID evidenciaId) {
    final SolicitacaoEvidenciaJpaEntity e = new SolicitacaoEvidenciaJpaEntity();
    e.setId(UUID.randomUUID());
    e.setSolicitacaoId(solicitacaoId);
    e.setEvidenciaId(evidenciaId);
    return e;
  }

  @Test
  void save_persisteERetorna() {
    final UUID solId = UUID.randomUUID();
    final UUID evId = UUID.randomUUID();
    final SolicitacaoEvidencia domain = new SolicitacaoEvidencia(solId, evId);
    final SolicitacaoEvidenciaJpaEntity saved = criarEntity(solId, evId);
    when(jpa.save(any(SolicitacaoEvidenciaJpaEntity.class))).thenReturn(saved);

    final SolicitacaoEvidencia result = adapter.save(domain);

    assertNotNull(result);
    assertEquals(solId, result.getSolicitacaoId());
    assertEquals(evId, result.getEvidenciaId());
    verify(jpa).save(any(SolicitacaoEvidenciaJpaEntity.class));
  }

  @Test
  void findBySolicitacaoId_retornaLista() {
    final UUID solId = UUID.randomUUID();
    final UUID evId = UUID.randomUUID();
    when(jpa.findBySolicitacaoId(solId)).thenReturn(List.of(criarEntity(solId, evId)));

    final List<SolicitacaoEvidencia> result = adapter.findBySolicitacaoId(solId);

    assertEquals(1, result.size());
    assertEquals(solId, result.get(0).getSolicitacaoId());
  }

  @Test
  void findBySolicitacaoId_quandoVazio_retornaListaVazia() {
    final UUID solId = UUID.randomUUID();
    when(jpa.findBySolicitacaoId(solId)).thenReturn(List.of());

    assertTrue(adapter.findBySolicitacaoId(solId).isEmpty());
  }

  @Test
  void existsBySolicitacaoIdAndEvidenciaId_retornaTrue() {
    final UUID solId = UUID.randomUUID();
    final UUID evId = UUID.randomUUID();
    when(jpa.existsBySolicitacaoIdAndEvidenciaId(solId, evId)).thenReturn(true);

    assertTrue(adapter.existsBySolicitacaoIdAndEvidenciaId(solId, evId));
  }

  @Test
  void existsBySolicitacaoIdAndEvidenciaId_retornaFalse() {
    final UUID solId = UUID.randomUUID();
    final UUID evId = UUID.randomUUID();
    when(jpa.existsBySolicitacaoIdAndEvidenciaId(solId, evId)).thenReturn(false);

    assertFalse(adapter.existsBySolicitacaoIdAndEvidenciaId(solId, evId));
  }

  @Test
  void deleteBySolicitacaoId_delegaAoJpa() {
    final UUID solId = UUID.randomUUID();
    adapter.deleteBySolicitacaoId(solId);
    verify(jpa).deleteBySolicitacaoId(solId);
  }

  @Test
  void deleteByEvidenciaId_delegaAoJpa() {
    final UUID evId = UUID.randomUUID();
    adapter.deleteByEvidenciaId(evId);
    verify(jpa).deleteByEvidenciaId(evId);
  }
}
