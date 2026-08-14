package com.rgm.api.adapter.out.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rgm.api.adapter.out.persistence.entity.FotoGaleriaModeloJpaEntity;
import com.rgm.api.core.domain.model.aggregates.FotoGaleriaModelo;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FotoGaleriaModeloMapperTest {

  @Test
  void toJpa_eToDomain_fazemRoundtrip() {
    final UUID id = UUID.randomUUID();
    final UUID modeloId = UUID.randomUUID();
    final UUID enviadaPor = UUID.randomUUID();
    final Instant agora = Instant.now();
    final FotoGaleriaModelo original =
        new FotoGaleriaModelo(
            id, modeloId, "http://minio/foto.jpg", "Parte 1", true, enviadaPor, agora);

    final FotoGaleriaModeloJpaEntity jpa = FotoGaleriaModeloMapper.toJpa(original);

    assertEquals(id, jpa.getId());
    assertEquals(modeloId, jpa.getModeloId());
    assertEquals("http://minio/foto.jpg", jpa.getPublicUrl());
    assertEquals("Parte 1", jpa.getIdentificacao());
    assertTrue(jpa.isPrincipal());
    assertEquals(enviadaPor, jpa.getEnviadaPorUsuarioId());

    final FotoGaleriaModelo result = FotoGaleriaModeloMapper.toDomain(jpa);

    assertEquals(id, result.getId());
    assertEquals(modeloId, result.getModeloId());
    assertEquals("http://minio/foto.jpg", result.getPublicUrl());
    assertEquals("Parte 1", result.getIdentificacao());
    assertTrue(result.isPrincipal());
    assertEquals(enviadaPor, result.getEnviadaPorUsuarioId());
    assertEquals(agora, result.getCriadoEm());
  }
}
