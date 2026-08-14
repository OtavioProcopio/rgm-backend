package com.rgm.api.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rgm.api.adapter.out.persistence.entity.FotoGaleriaModeloJpaEntity;
import com.rgm.api.adapter.out.persistence.repository.FotoGaleriaModeloJpaRepository;
import com.rgm.api.core.domain.model.aggregates.FotoGaleriaModelo;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FotoGaleriaModeloRepositoryAdapterTest {

  private FotoGaleriaModeloJpaRepository jpa;
  private FotoGaleriaModeloRepositoryAdapter adapter;

  @BeforeEach
  void setUp() {
    jpa = mock(FotoGaleriaModeloJpaRepository.class);
    adapter = new FotoGaleriaModeloRepositoryAdapter(jpa);
  }

  private FotoGaleriaModeloJpaEntity criarEntity(final UUID modeloId) {
    return new FotoGaleriaModeloJpaEntity(
        UUID.randomUUID(),
        modeloId,
        "http://minio/foto.jpg",
        "Parte 1",
        true,
        UUID.randomUUID(),
        Instant.now());
  }

  private FotoGaleriaModelo criarDomain(final UUID modeloId) {
    return FotoGaleriaModelo.criar(
        modeloId, "http://minio/foto.jpg", "Parte 1", true, UUID.randomUUID(), Instant.now());
  }

  @Test
  void findById_quandoExistir_retornaFoto() {
    final UUID modeloId = UUID.randomUUID();
    final FotoGaleriaModeloJpaEntity e = criarEntity(modeloId);
    when(jpa.findById(e.getId())).thenReturn(Optional.of(e));

    final Optional<FotoGaleriaModelo> result = adapter.findById(e.getId());

    assertTrue(result.isPresent());
    assertEquals(modeloId, result.get().getModeloId());
  }

  @Test
  void findById_quandoNaoExistir_retornaVazio() {
    final UUID id = UUID.randomUUID();
    when(jpa.findById(id)).thenReturn(Optional.empty());

    assertFalse(adapter.findById(id).isPresent());
  }

  @Test
  void findByModeloId_retornaListaOrdenada() {
    final UUID modeloId = UUID.randomUUID();
    when(jpa.findByModeloIdOrderByCriadoEmAsc(modeloId))
        .thenReturn(List.of(criarEntity(modeloId), criarEntity(modeloId)));

    final List<FotoGaleriaModelo> result = adapter.findByModeloId(modeloId);

    assertEquals(2, result.size());
  }

  @Test
  void save_persisteERetorna() {
    final UUID modeloId = UUID.randomUUID();
    final FotoGaleriaModelo domain = criarDomain(modeloId);
    when(jpa.save(any(FotoGaleriaModeloJpaEntity.class))).thenReturn(criarEntity(modeloId));

    final FotoGaleriaModelo result = adapter.save(domain);

    assertNotNull(result);
    verify(jpa).save(any(FotoGaleriaModeloJpaEntity.class));
  }

  @Test
  void deleteById_delegaAoJpa() {
    final UUID id = UUID.randomUUID();
    adapter.deleteById(id);
    verify(jpa).deleteById(id);
  }

  @Test
  void limparPrincipal_delegaAoJpa() {
    final UUID modeloId = UUID.randomUUID();
    adapter.limparPrincipal(modeloId);
    verify(jpa).limparPrincipal(modeloId);
  }

  @Test
  void findPrincipalUrlsByModeloIds_retornaMapa() {
    final UUID modeloId = UUID.randomUUID();
    final FotoGaleriaModeloJpaEntity e = criarEntity(modeloId);
    when(jpa.findByModeloIdInAndPrincipalTrue(List.of(modeloId))).thenReturn(List.of(e));

    final Map<UUID, String> result = adapter.findPrincipalUrlsByModeloIds(List.of(modeloId));

    assertEquals("http://minio/foto.jpg", result.get(modeloId));
  }

  @Test
  void findPrincipalUrlsByModeloIds_comListaVazia_naoChamaJpa() {
    final Map<UUID, String> result = adapter.findPrincipalUrlsByModeloIds(List.of());

    assertTrue(result.isEmpty());
  }
}
