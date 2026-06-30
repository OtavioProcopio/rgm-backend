package com.rgm.api.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rgm.api.adapter.out.persistence.entity.EvidenciaJpaEntity;
import com.rgm.api.adapter.out.persistence.repository.EvidenciaJpaRepository;
import com.rgm.api.core.domain.model.aggregates.Evidencia;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EvidenciaRepositoryAdapterTest {

  private EvidenciaJpaRepository jpa;
  private EvidenciaRepositoryAdapter adapter;

  @BeforeEach
  void setUp() {
    jpa = mock(EvidenciaJpaRepository.class);
    adapter = new EvidenciaRepositoryAdapter(jpa);
  }

  private EvidenciaJpaEntity criarEntity() {
    final EvidenciaJpaEntity e = new EvidenciaJpaEntity();
    e.setId(UUID.randomUUID());
    e.setPublicUrl("http://minio/foto.jpg");
    e.setMimeType("image/jpeg");
    e.setNomeArquivo("foto.jpg");
    e.setTamanhoBytes(1024);
    e.setEnviadaPorUsuarioId(UUID.randomUUID());
    e.setCriadaEm(Instant.now());
    return e;
  }

  private Evidencia criarDomain() {
    return new Evidencia(
        UUID.randomUUID(),
        "http://minio/foto.jpg",
        "image/jpeg",
        "foto.jpg",
        1024,
        UUID.randomUUID(),
        Instant.now());
  }

  @Test
  void findById_quandoExistir_retornaEvidencia() {
    final UUID id = UUID.randomUUID();
    final EvidenciaJpaEntity e = criarEntity();
    e.setId(id);
    when(jpa.findById(id)).thenReturn(Optional.of(e));

    final Optional<Evidencia> result = adapter.findById(id);

    assertTrue(result.isPresent());
    assertNotNull(result.get().getPublicUrl());
  }

  @Test
  void findById_quandoNaoExistir_retornaVazio() {
    final UUID id = UUID.randomUUID();
    when(jpa.findById(id)).thenReturn(Optional.empty());

    assertFalse(adapter.findById(id).isPresent());
  }

  @Test
  void save_persisteERetorna() {
    final Evidencia domain = criarDomain();
    final EvidenciaJpaEntity e = criarEntity();
    when(jpa.save(any(EvidenciaJpaEntity.class))).thenReturn(e);

    final Evidencia result = adapter.save(domain);

    assertNotNull(result);
    verify(jpa).save(any(EvidenciaJpaEntity.class));
  }

  @Test
  void deleteById_delegaAoJpa() {
    final UUID id = UUID.randomUUID();
    adapter.deleteById(id);
    verify(jpa).deleteById(id);
  }
}
