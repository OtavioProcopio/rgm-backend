package com.rgm.api.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rgm.api.adapter.out.persistence.entity.ModeloJpaEntity;
import com.rgm.api.adapter.out.persistence.repository.ModeloJpaRepository;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class ModeloRepositoryAdapterTest {

  private ModeloJpaRepository jpa;
  private ModeloRepositoryAdapter adapter;

  @BeforeEach
  void setUp() {
    jpa = mock(ModeloJpaRepository.class);
    adapter = new ModeloRepositoryAdapter(jpa);
  }

  private ModeloJpaEntity criarJpaEntity() {
    final ModeloJpaEntity entity = new ModeloJpaEntity();
    entity.setId(UUID.randomUUID());
    entity.setCodigo("MOD-001");
    entity.setVersao(1);
    entity.setDescricao("Desc");
    entity.setObservacoes("Obs");
    entity.setAtivo(true);
    entity.setMaquina("MANUAL");
    entity.setTemPendenciaAberta(false);
    entity.setCriadoEm(Instant.now());
    entity.setAtualizadoEm(Instant.now());
    return entity;
  }

  private Modelo criarModeloDomain() {
    return new Modelo(
        UUID.randomUUID(),
        "MOD-001",
        1,
        "Desc",
        "Obs",
        null,
        null,
        null,
        null,
        true,
        "MANUAL",
        false,
        Instant.now(),
        Instant.now());
  }

  @Test
  void findById_quandoExistir_deveRetornarModelo() {
    final UUID id = UUID.randomUUID();
    final ModeloJpaEntity entity = criarJpaEntity();
    entity.setId(id);
    when(jpa.findById(id)).thenReturn(Optional.of(entity));

    final Optional<Modelo> result = adapter.findById(id);

    assertTrue(result.isPresent());
    assertEquals("MOD-001", result.get().getCodigo());
    verify(jpa).findById(id);
  }

  @Test
  void findById_quandoNaoExistir_deveRetornarOptionalVazio() {
    final UUID id = UUID.randomUUID();
    when(jpa.findById(id)).thenReturn(Optional.empty());

    final Optional<Modelo> result = adapter.findById(id);

    assertFalse(result.isPresent());
    verify(jpa).findById(id);
  }

  @Test
  void save_devePersistirERetornarModeloMapped() {
    final Modelo domain = criarModeloDomain();
    final ModeloJpaEntity entity = criarJpaEntity();
    when(jpa.save(any(ModeloJpaEntity.class))).thenReturn(entity);

    final Modelo result = adapter.save(domain);

    assertNotNull(result);
    assertEquals("MOD-001", result.getCodigo());
    verify(jpa).save(any(ModeloJpaEntity.class));
  }

  @Test
  void deleteById_deveDelegarAoJpa() {
    final UUID id = UUID.randomUUID();
    doNothing().when(jpa).deleteById(id);

    adapter.deleteById(id);

    verify(jpa).deleteById(id);
  }

  @Test
  void countByMaquinaAndCodigo_deveDelegarAoJpa() {
    when(jpa.countByMaquinaAndCodigo("MANUAL", "MOD-001")).thenReturn(5);

    final int result = adapter.countByMaquinaAndCodigo("MANUAL", "MOD-001");

    assertEquals(5, result);
    verify(jpa).countByMaquinaAndCodigo("MANUAL", "MOD-001");
  }

  @Test
  void count_deveDelegarAoJpa() {
    when(jpa.count()).thenReturn(10L);

    final long result = adapter.count();

    assertEquals(10L, result);
    verify(jpa).count();
  }

  @Test
  void findAll_deveRetornarPaginadoMapped() {
    final ModeloJpaEntity entity = criarJpaEntity();
    final Page<ModeloJpaEntity> page = new PageImpl<>(List.of(entity), PageRequest.of(0, 10), 1);
    when(jpa.findAll(any(PageRequest.class))).thenReturn(page);

    final PageResult<Modelo> result = adapter.findAll(0, 10);

    assertNotNull(result);
    assertEquals(1, result.content().size());
    assertEquals("MOD-001", result.content().get(0).getCodigo());
    verify(jpa).findAll(any(PageRequest.class));
  }

  @Test
  void findByFilters_deveRetornarPaginadoMapped() {
    final ModeloJpaEntity entity = criarJpaEntity();
    final Page<ModeloJpaEntity> page = new PageImpl<>(List.of(entity), PageRequest.of(0, 10), 1);
    when(jpa.findByFilters(any(), any(), any(), any(), any(PageRequest.class))).thenReturn(page);

    final PageResult<Modelo> result =
        adapter.findByFilters(true, "MOD-001", "MANUAL", "Desc", 0, 10);

    assertNotNull(result);
    assertEquals(1, result.content().size());
    assertEquals("MOD-001", result.content().get(0).getCodigo());
    verify(jpa).findByFilters(any(), any(), any(), any(), any(PageRequest.class));
  }
}
