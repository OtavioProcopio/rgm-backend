package com.rgm.api.adapter.out.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rgm.api.adapter.out.persistence.entity.ModeloJpaEntity;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class ModeloJpaRepositoryTest {

  @Autowired private ModeloJpaRepository repository;

  @BeforeEach
  void setUp() {
    repository.deleteAll();
  }

  private ModeloJpaEntity persistirModelo(
      final String codigo, final String maquina, final String descricao, final boolean ativo) {
    final ModeloJpaEntity m = new ModeloJpaEntity();
    m.setId(UUID.randomUUID());
    m.setCodigo(codigo);
    m.setVersao(1);
    m.setDescricao(descricao);
    m.setObservacoes("Obs");
    m.setAtivo(ativo);
    m.setMaquina(maquina);
    m.setTemPendenciaAberta(false);
    m.setCriadoEm(Instant.now());
    m.setAtualizadoEm(Instant.now());
    return repository.save(m);
  }

  @Test
  void findByFilters_semFiltros_deveRetornarTodos() {
    persistirModelo("MOD-01", "INJETORA-A", "Modelo A", true);
    persistirModelo("MOD-02", "INJETORA-B", "Modelo B", false);

    final Pageable pageable = PageRequest.of(0, 10);
    final Page<ModeloJpaEntity> page = repository.findByFilters(null, null, null, null, pageable);

    assertNotNull(page);
    assertEquals(2, page.getTotalElements());
  }

  @Test
  void findByFilters_filtrarAtivo_deveRetornarApenasAtivos() {
    persistirModelo("MOD-01", "INJETORA-A", "Modelo A", true);
    persistirModelo("MOD-02", "INJETORA-B", "Modelo B", false);

    final Pageable pageable = PageRequest.of(0, 10);
    final Page<ModeloJpaEntity> page = repository.findByFilters(true, null, null, null, pageable);

    assertNotNull(page);
    assertEquals(1, page.getTotalElements());
    assertTrue(page.getContent().get(0).isAtivo());
  }

  @Test
  void findByFilters_filtrarCodigo_deveFazerMatchCaseInsensitive() {
    persistirModelo("MOD-01", "INJETORA-A", "Modelo A", true);
    persistirModelo("MOD-02", "INJETORA-B", "Modelo B", true);

    final Pageable pageable = PageRequest.of(0, 10);
    final Page<ModeloJpaEntity> page = repository.findByFilters(null, "od-01", null, null, pageable);

    assertNotNull(page);
    assertEquals(1, page.getTotalElements());
    assertEquals("MOD-01", page.getContent().get(0).getCodigo());
  }

  @Test
  void findByFilters_filtrarMaquina_deveFazerMatchCaseInsensitive() {
    persistirModelo("MOD-01", "INJETORA-A", "Modelo A", true);
    persistirModelo("MOD-02", "PRENSA-B", "Modelo B", true);

    final Pageable pageable = PageRequest.of(0, 10);
    final Page<ModeloJpaEntity> page = repository.findByFilters(null, null, "jetora", null, pageable);

    assertNotNull(page);
    assertEquals(1, page.getTotalElements());
    assertEquals("INJETORA-A", page.getContent().get(0).getMaquina());
  }

  @Test
  void findByFilters_filtrarDescricao_deveFazerMatchCaseInsensitive() {
    persistirModelo("MOD-01", "INJETORA-A", "Modelo de Teste A", true);
    persistirModelo("MOD-02", "PRENSA-B", "Modelo de Produção B", true);

    final Pageable pageable = PageRequest.of(0, 10);
    final Page<ModeloJpaEntity> page = repository.findByFilters(null, null, null, "de teste", pageable);

    assertNotNull(page);
    assertEquals(1, page.getTotalElements());
    assertEquals("Modelo de Teste A", page.getContent().get(0).getDescricao());
  }

  @Test
  void findByFilters_multiplosFiltros_deveCombinarFiltrosComAND() {
    persistirModelo("MOD-01", "INJETORA-A", "Modelo de Teste A", true);
    persistirModelo("MOD-02", "INJETORA-A", "Modelo de Produção B", true);
    persistirModelo("MOD-03", "PRENSA-B", "Modelo de Teste A", true);

    final Pageable pageable = PageRequest.of(0, 10);
    final Page<ModeloJpaEntity> page = repository.findByFilters(true, "MOD", "INJETORA", "teste", pageable);

    assertNotNull(page);
    assertEquals(1, page.getTotalElements());
    assertEquals("MOD-01", page.getContent().get(0).getCodigo());
  }
}
