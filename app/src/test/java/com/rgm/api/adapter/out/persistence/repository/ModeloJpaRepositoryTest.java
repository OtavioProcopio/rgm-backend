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
    final Page<ModeloJpaEntity> page =
        repository.findByFilters(null, "od-01", null, null, pageable);

    assertNotNull(page);
    assertEquals(1, page.getTotalElements());
    assertEquals("MOD-01", page.getContent().get(0).getCodigo());
  }

  @Test
  void findByFilters_filtrarMaquina_deveFazerMatchCaseInsensitive() {
    persistirModelo("MOD-01", "INJETORA-A", "Modelo A", true);
    persistirModelo("MOD-02", "PRENSA-B", "Modelo B", true);

    final Pageable pageable = PageRequest.of(0, 10);
    final Page<ModeloJpaEntity> page =
        repository.findByFilters(null, null, "jetora", null, pageable);

    assertNotNull(page);
    assertEquals(1, page.getTotalElements());
    assertEquals("INJETORA-A", page.getContent().get(0).getMaquina());
  }

  @Test
  void findByFilters_filtrarDescricao_deveFazerMatchCaseInsensitive() {
    persistirModelo("MOD-01", "INJETORA-A", "Modelo de Teste A", true);
    persistirModelo("MOD-02", "PRENSA-B", "Modelo de Produção B", true);

    final Pageable pageable = PageRequest.of(0, 10);
    final Page<ModeloJpaEntity> page =
        repository.findByFilters(null, null, null, "de teste", pageable);

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
    final Page<ModeloJpaEntity> page =
        repository.findByFilters(true, "MOD", "INJETORA", "teste", pageable);

    assertNotNull(page);
    assertEquals(1, page.getTotalElements());
    assertEquals("MOD-01", page.getContent().get(0).getCodigo());
  }

  private void persistirModeloComPendencia(final String codigo, final String maquina) {
    final ModeloJpaEntity m = persistirModelo(codigo, maquina, "Com pendencia", true);
    m.setTemPendenciaAberta(true);
    repository.save(m);
  }

  private void persistirCadastroDeSeteModelos() {
    persistirModelo("M1", "FBOX", "d", true);
    persistirModelo("M2", "FBOX", "d", false);
    persistirModeloComPendencia("M3", "FBOX");
    persistirModelo("M4", "DISA", "d", true);
    persistirModelo("M5", "DISA", "d", true);
    persistirModelo("M6", "DISA", "d", false);
    persistirModeloComPendencia("M7", "DISA");
  }

  @Test
  void shouldContarTotalAtivosEComPendenciaWhenResumeContagens() {
    // Arrange
    persistirCadastroDeSeteModelos();

    // Act
    final Object[] row = repository.resumirContagens().get(0);

    // Assert
    assertEquals(7L, ((Number) row[0]).longValue());
    assertEquals(5L, ((Number) row[1]).longValue());
    assertEquals(2L, ((Number) row[2]).longValue());
  }

  @Test
  void shouldDevolverContagensZeradasWhenNaoHaModelo() {
    // Arrange
    repository.deleteAll();

    // Act
    final Object[] row = repository.resumirContagens().get(0);

    // Assert
    assertEquals(0L, ((Number) row[0]).longValue());
    assertEquals(0L, ((Number) row[1]).longValue());
    assertEquals(0L, ((Number) row[2]).longValue());
  }

  @Test
  void shouldOrdenarDaMaiorParaAMenorQuantidadeWhenContaPorMaquina() {
    // Arrange
    persistirCadastroDeSeteModelos();

    // Act
    final var linhas = repository.contarPorMaquina();

    // Assert
    assertEquals(2, linhas.size());
    assertEquals("DISA", linhas.get(0)[0]);
    assertEquals(4L, ((Number) linhas.get(0)[1]).longValue());
    assertEquals("FBOX", linhas.get(1)[0]);
    assertEquals(3L, ((Number) linhas.get(1)[1]).longValue());
  }

  @Test
  void shouldDesempatarEmOrdemAlfabeticaWhenMaquinasTemAMesmaQuantidade() {
    // Arrange
    persistirModelo("M1", "SINTO", "d", true);
    persistirModelo("M2", "DISA", "d", true);
    persistirModelo("M3", "FBOX", "d", false);

    // Act
    final var maquinas = repository.contarPorMaquina().stream().map(l -> l[0]).toList();

    // Assert
    assertEquals(java.util.List.of("DISA", "FBOX", "SINTO"), maquinas);
  }
}
