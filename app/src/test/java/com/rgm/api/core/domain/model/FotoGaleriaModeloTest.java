package com.rgm.api.core.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import com.rgm.api.core.domain.model.aggregates.FotoGaleriaModelo;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FotoGaleriaModeloTest {

  private static final Instant AGORA = Instant.now();

  @Test
  void deveCriarFotoComValoresCorretos() {
    final UUID modeloId = UUID.randomUUID();
    final UUID usuarioId = UUID.randomUUID();
    final FotoGaleriaModelo foto =
        FotoGaleriaModelo.criar(
            modeloId, "http://minio/foto.jpg", "Parte 1", true, usuarioId, AGORA);

    assertNotNull(foto.getId());
    assertEquals(modeloId, foto.getModeloId());
    assertEquals("http://minio/foto.jpg", foto.getPublicUrl());
    assertEquals("Parte 1", foto.getIdentificacao());
    assertTrue(foto.isPrincipal());
    assertEquals(usuarioId, foto.getEnviadaPorUsuarioId());
    assertEquals(AGORA, foto.getCriadoEm());
  }

  @Test
  void deveAceitarEnviadaPorUsuarioIdNulo() {
    final FotoGaleriaModelo foto =
        FotoGaleriaModelo.criar(
            UUID.randomUUID(), "http://minio/foto.jpg", "Parte 1", false, null, AGORA);

    assertNull(foto.getEnviadaPorUsuarioId());
  }

  @Test
  void deveFalharSemPublicUrl() {
    assertThrows(
        IllegalArgumentException.class,
        () -> FotoGaleriaModelo.criar(UUID.randomUUID(), "", "Parte 1", false, null, AGORA));
  }

  @Test
  void deveFalharSemIdentificacao() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            FotoGaleriaModelo.criar(
                UUID.randomUUID(), "http://minio/foto.jpg", "", false, null, AGORA));
  }

  @Test
  void deveRenomearIdentificacao() {
    final FotoGaleriaModelo foto =
        FotoGaleriaModelo.criar(
            UUID.randomUUID(), "http://minio/foto.jpg", "Parte 1", false, null, AGORA);
    final FotoGaleriaModelo renomeada = foto.comIdentificacao("Contra-macho");

    assertEquals("Contra-macho", renomeada.getIdentificacao());
    assertEquals(foto.getId(), renomeada.getId());
  }

  @Test
  void deveMarcarEDesmarcarPrincipal() {
    final FotoGaleriaModelo foto =
        FotoGaleriaModelo.criar(
            UUID.randomUUID(), "http://minio/foto.jpg", "Parte 1", false, null, AGORA);

    final FotoGaleriaModelo marcada = foto.comPrincipal(true);
    assertTrue(marcada.isPrincipal());

    final FotoGaleriaModelo desmarcada = marcada.comPrincipal(false);
    assertFalse(desmarcada.isPrincipal());
  }
}
