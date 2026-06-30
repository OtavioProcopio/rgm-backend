package com.rgm.api.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rgm.api.adapter.out.persistence.entity.SolicitacaoAtribuicaoJpaEntity;
import com.rgm.api.adapter.out.persistence.repository.SolicitacaoAtribuicaoJpaRepository;
import com.rgm.api.core.domain.model.entities.SolicitacaoAtribuicao;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SolicitacaoAtribuicaoRepositoryAdapterTest {

  private SolicitacaoAtribuicaoJpaRepository jpa;
  private SolicitacaoAtribuicaoRepositoryAdapter adapter;

  @BeforeEach
  void setUp() {
    jpa = mock(SolicitacaoAtribuicaoJpaRepository.class);
    adapter = new SolicitacaoAtribuicaoRepositoryAdapter(jpa);
  }

  private SolicitacaoAtribuicaoJpaEntity criarEntity(
      final UUID solicitacaoId, final UUID usuarioId) {
    final SolicitacaoAtribuicaoJpaEntity e = new SolicitacaoAtribuicaoJpaEntity();
    e.setId(UUID.randomUUID());
    e.setSolicitacaoId(solicitacaoId);
    e.setUsuarioId(usuarioId);
    e.setAtribuidoPorUsuarioId(UUID.randomUUID());
    e.setAtribuidoEm(Instant.now());
    return e;
  }

  private SolicitacaoAtribuicao criarDomain(final UUID solicitacaoId, final UUID usuarioId) {
    return new SolicitacaoAtribuicao(
        UUID.randomUUID(), solicitacaoId, usuarioId, UUID.randomUUID(), Instant.now(), null);
  }

  @Test
  void save_persisteERetorna() {
    final UUID solId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final SolicitacaoAtribuicao domain = criarDomain(solId, userId);
    final SolicitacaoAtribuicaoJpaEntity e = criarEntity(solId, userId);
    when(jpa.save(any(SolicitacaoAtribuicaoJpaEntity.class))).thenReturn(e);

    final SolicitacaoAtribuicao result = adapter.save(domain);

    assertNotNull(result);
    verify(jpa).save(any(SolicitacaoAtribuicaoJpaEntity.class));
  }

  @Test
  void findBySolicitacaoId_retornaLista() {
    final UUID solId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    when(jpa.findBySolicitacaoId(solId)).thenReturn(List.of(criarEntity(solId, userId)));

    final List<SolicitacaoAtribuicao> result = adapter.findBySolicitacaoId(solId);

    assertEquals(1, result.size());
    assertEquals(solId, result.get(0).getSolicitacaoId());
  }

  @Test
  void findBySolicitacaoIdIn_retornaLista() {
    final UUID solId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    when(jpa.findBySolicitacaoIdIn(List.of(solId))).thenReturn(List.of(criarEntity(solId, userId)));

    final List<SolicitacaoAtribuicao> result = adapter.findBySolicitacaoIdIn(List.of(solId));

    assertEquals(1, result.size());
  }

  @Test
  void existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull_retornaTrue() {
    final UUID solId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    when(jpa.existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(solId, userId)).thenReturn(true);

    assertTrue(adapter.existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(solId, userId));
  }

  @Test
  void existsByUsuarioIdAndRemovidoEmIsNull_retornaFalse() {
    final UUID userId = UUID.randomUUID();
    when(jpa.existsByUsuarioIdAndRemovidoEmIsNull(userId)).thenReturn(false);

    assertFalse(adapter.existsByUsuarioIdAndRemovidoEmIsNull(userId));
  }

  @Test
  void deleteBySolicitacaoId_delegaAoJpa() {
    final UUID solId = UUID.randomUUID();
    adapter.deleteBySolicitacaoId(solId);
    verify(jpa).deleteBySolicitacaoId(solId);
  }
}
