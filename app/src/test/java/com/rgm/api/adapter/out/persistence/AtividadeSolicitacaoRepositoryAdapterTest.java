package com.rgm.api.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rgm.api.adapter.out.persistence.entity.AtividadeSolicitacaoJpaEntity;
import com.rgm.api.adapter.out.persistence.repository.AtividadeSolicitacaoJpaRepository;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoAtividadeSolicitacao;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AtividadeSolicitacaoRepositoryAdapterTest {

  private AtividadeSolicitacaoJpaRepository jpa;
  private AtividadeSolicitacaoRepositoryAdapter adapter;

  @BeforeEach
  void setUp() {
    jpa = mock(AtividadeSolicitacaoJpaRepository.class);
    adapter = new AtividadeSolicitacaoRepositoryAdapter(jpa);
  }

  private AtividadeSolicitacaoJpaEntity criarEntity(final UUID solicitacaoId) {
    final AtividadeSolicitacaoJpaEntity e = new AtividadeSolicitacaoJpaEntity();
    e.setId(UUID.randomUUID());
    e.setSolicitacaoId(solicitacaoId);
    e.setTipo(TipoAtividadeSolicitacao.MUDANCA_STATUS);
    e.setDeStatus(StatusSolicitacao.A_FAZER);
    e.setParaStatus(StatusSolicitacao.EM_ANDAMENTO);
    e.setAutorUsuarioId(UUID.randomUUID());
    e.setCriadaEm(Instant.now());
    return e;
  }

  private AtividadeSolicitacao criarDomain(final UUID solicitacaoId) {
    return new AtividadeSolicitacao(
        UUID.randomUUID(),
        solicitacaoId,
        TipoAtividadeSolicitacao.MUDANCA_STATUS,
        StatusSolicitacao.A_FAZER,
        StatusSolicitacao.EM_ANDAMENTO,
        null,
        UUID.randomUUID(),
        Instant.now());
  }

  @Test
  void save_persisteERetorna() {
    final UUID solicitacaoId = UUID.randomUUID();
    final AtividadeSolicitacao domain = criarDomain(solicitacaoId);
    final AtividadeSolicitacaoJpaEntity e = criarEntity(solicitacaoId);
    when(jpa.save(any(AtividadeSolicitacaoJpaEntity.class))).thenReturn(e);

    final AtividadeSolicitacao result = adapter.save(domain);

    assertNotNull(result);
    verify(jpa).save(any(AtividadeSolicitacaoJpaEntity.class));
  }

  @Test
  void findBySolicitacaoId_retornaLista() {
    final UUID solicitacaoId = UUID.randomUUID();
    final AtividadeSolicitacaoJpaEntity e = criarEntity(solicitacaoId);
    when(jpa.findBySolicitacaoId(solicitacaoId)).thenReturn(List.of(e));

    final List<AtividadeSolicitacao> result = adapter.findBySolicitacaoId(solicitacaoId);

    assertEquals(1, result.size());
    assertEquals(solicitacaoId, result.get(0).getSolicitacaoId());
  }

  @Test
  void existsByAutorId_quandoExistir_retornaTrue() {
    final UUID autorId = UUID.randomUUID();
    when(jpa.existsByAutorUsuarioId(autorId)).thenReturn(true);

    assertTrue(adapter.existsByAutorId(autorId));
  }

  @Test
  void existsByAutorId_quandoNaoExistir_retornaFalse() {
    final UUID autorId = UUID.randomUUID();
    when(jpa.existsByAutorUsuarioId(autorId)).thenReturn(false);

    assertFalse(adapter.existsByAutorId(autorId));
  }

  @Test
  void deleteBySolicitacaoId_delegaAoJpa() {
    final UUID solicitacaoId = UUID.randomUUID();
    adapter.deleteBySolicitacaoId(solicitacaoId);
    verify(jpa).deleteBySolicitacaoId(solicitacaoId);
  }
}
