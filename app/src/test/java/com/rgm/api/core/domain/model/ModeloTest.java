package com.rgm.api.core.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import com.rgm.api.core.domain.model.aggregates.Modelo;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ModeloTest {

  private static final Instant AGORA = Instant.now();
  private static final String MAQUINA = "FBOX";

  @Test
  void deveCriarModeloComValoresCorretos() {
    final Modelo modelo = Modelo.criar("COD-001", "Descricao", "Obs", MAQUINA, 1, AGORA);

    assertNotNull(modelo.getId());
    assertEquals("COD-001", modelo.getCodigo());
    assertEquals(1, modelo.getVersao());
    assertEquals("Descricao", modelo.getDescricao());
    assertEquals("Obs", modelo.getObservacoes());
    assertNull(modelo.getFotoUrl());
    assertTrue(modelo.isAtivo());
    assertFalse(modelo.isTemPendenciaAberta());
    assertEquals(MAQUINA, modelo.getMaquina());
  }

  @Test
  void deveFalharSemCodigo() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Modelo.criar("", "Descricao", null, MAQUINA, 1, AGORA));
  }

  @Test
  void deveFalharSemDescricao() {
    assertThrows(
        IllegalArgumentException.class, () -> Modelo.criar("COD", "", null, MAQUINA, 1, AGORA));
  }

  @Test
  void deveDesativarModelo() {
    final Modelo modelo = Modelo.criar("COD", "Desc", null, MAQUINA, 1, AGORA);
    final Modelo desativado = modelo.desativar(AGORA);

    assertFalse(desativado.isAtivo());
    assertEquals(modelo.getId(), desativado.getId());
  }

  @Test
  void deveAtualizarFotoUrl() {
    final Modelo modelo = Modelo.criar("COD", "Desc", null, MAQUINA, 1, AGORA);
    final Modelo atualizado = modelo.withFotoUrl("http://foto.jpg", AGORA);

    assertEquals("http://foto.jpg", atualizado.getFotoUrl());
    assertEquals(AGORA, atualizado.getFotoAtualizadaEm());
  }

  @Test
  void deveEditarModelo() {
    final Modelo modelo = Modelo.criar("COD", "Desc", null, MAQUINA, 1, AGORA);
    final Modelo editado = modelo.editar("COD-NEW", "Nova Desc", "Obs nova", "FAST-LOOP", AGORA);

    assertEquals("COD-NEW", editado.getCodigo());
    assertEquals("Nova Desc", editado.getDescricao());
    assertEquals("Obs nova", editado.getObservacoes());
    assertEquals("FAST-LOOP", editado.getMaquina());
  }

  @Test
  void deveAtualizarPendenciaAberta() {
    final Modelo modelo = Modelo.criar("COD", "Desc", null, MAQUINA, 1, AGORA);
    final Modelo comPendencia = modelo.withTemPendenciaAberta(true, AGORA);

    assertTrue(comPendencia.isTemPendenciaAberta());
  }
}
