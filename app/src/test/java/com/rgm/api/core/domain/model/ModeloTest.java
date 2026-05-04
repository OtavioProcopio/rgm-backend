package com.rgm.api.core.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import com.rgm.api.core.domain.model.aggregates.Modelo;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ModeloTest {

  private static final Instant AGORA = Instant.now();
  private static final UUID MAQUINA_ID = UUID.randomUUID();

  @Test
  void deveCriarModeloComValoresCorretos() {
    final Modelo modelo = Modelo.criar("COD-001", "Descricao", "Obs", MAQUINA_ID, 1, AGORA);

    assertNotNull(modelo.getId());
    assertEquals("COD-001", modelo.getCodigo());
    assertEquals(1, modelo.getVersao());
    assertEquals("Descricao", modelo.getDescricao());
    assertEquals("Obs", modelo.getObservacoes());
    assertNull(modelo.getFotoUrl());
    assertTrue(modelo.isAtivo());
    assertFalse(modelo.isTemPendenciaAberta());
    assertEquals(MAQUINA_ID, modelo.getMaquinaId());
  }

  @Test
  void deveFalharSemCodigo() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Modelo.criar("", "Descricao", null, MAQUINA_ID, 1, AGORA));
  }

  @Test
  void deveFalharSemDescricao() {
    assertThrows(
        IllegalArgumentException.class, () -> Modelo.criar("COD", "", null, MAQUINA_ID, 1, AGORA));
  }

  @Test
  void deveDesativarModelo() {
    final Modelo modelo = Modelo.criar("COD", "Desc", null, MAQUINA_ID, 1, AGORA);
    final Modelo desativado = modelo.desativar(AGORA);

    assertFalse(desativado.isAtivo());
    assertEquals(modelo.getId(), desativado.getId());
  }

  @Test
  void deveAtualizarFotoUrl() {
    final Modelo modelo = Modelo.criar("COD", "Desc", null, MAQUINA_ID, 1, AGORA);
    final Modelo atualizado = modelo.withFotoUrl("http://foto.jpg", AGORA);

    assertEquals("http://foto.jpg", atualizado.getFotoUrl());
    assertEquals(AGORA, atualizado.getFotoAtualizadaEm());
  }

  @Test
  void deveEditarModelo() {
    final Modelo modelo = Modelo.criar("COD", "Desc", null, MAQUINA_ID, 1, AGORA);
    final Modelo editado = modelo.editar("COD-NEW", "Nova Desc", "Obs nova", AGORA);

    assertEquals("COD-NEW", editado.getCodigo());
    assertEquals("Nova Desc", editado.getDescricao());
    assertEquals("Obs nova", editado.getObservacoes());
  }

  @Test
  void deveAtualizarPendenciaAberta() {
    final Modelo modelo = Modelo.criar("COD", "Desc", null, MAQUINA_ID, 1, AGORA);
    final Modelo comPendencia = modelo.withTemPendenciaAberta(true, AGORA);

    assertTrue(comPendencia.isTemPendenciaAberta());
  }
}
