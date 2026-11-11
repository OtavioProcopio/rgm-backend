package com.rgm.api.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rgm.api.adapter.out.persistence.entity.ModeloJpaEntity;
import com.rgm.api.adapter.out.persistence.repository.ModeloJpaRepository;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.ports.repositories.ContagensDeModelos;
import com.rgm.api.core.domain.ports.repositories.FiltrosDeModelos;
import com.rgm.api.core.domain.ports.repositories.OrdemDeModelos;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import com.rgm.api.core.domain.ports.repositories.PosicaoDoModelo;
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
        true,
        "MANUAL",
        null,
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
  void shouldDelegateLargestVersionToJpaWhenAskingForMaiorVersao() {
    // Arrange
    when(jpa.maiorVersao("MANUAL", "MOD-001")).thenReturn(5);

    // Act
    final int result = adapter.maiorVersao("MANUAL", "MOD-001");

    // Assert
    assertEquals(5, result);
    verify(jpa, times(1)).maiorVersao("MANUAL", "MOD-001");
  }

  @Test
  void shouldDelegateIdentificationCheckToJpaWhenLookingForAnotherModelo() {
    // Arrange
    final UUID id = UUID.randomUUID();
    when(jpa.existsByMaquinaAndCodigoAndVersaoAndIdNot("MANUAL", "MOD-001", 2, id))
        .thenReturn(true);

    // Act
    final boolean result = adapter.existeOutroComIdentificacao("MANUAL", "MOD-001", 2, id);

    // Assert
    assertTrue(result);
    verify(jpa, times(1)).existsByMaquinaAndCodigoAndVersaoAndIdNot("MANUAL", "MOD-001", 2, id);
  }

  @Test
  void shouldMapEntitiesWhenFindingAllByIdIn() {
    // Arrange
    final ModeloJpaEntity entity = criarJpaEntity();
    final List<UUID> ids = List.of(entity.getId());
    when(jpa.findAllById(ids)).thenReturn(List.of(entity));

    // Act
    final List<Modelo> result = adapter.findAllByIdIn(ids);

    // Assert
    assertEquals(1, result.size());
    assertEquals(entity.getId(), result.get(0).getId());
    verify(jpa, times(1)).findAllById(ids);
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

  @Test
  void shouldMontarOResumoComInativosCalculadosWhenResume() {
    // Arrange
    final List<Object[]> contagens = new java.util.ArrayList<>();
    contagens.add(new Object[] {7L, 5L, 2L});
    final List<Object[]> porMaquina = new java.util.ArrayList<>();
    porMaquina.add(new Object[] {"DISA", 4L});
    porMaquina.add(new Object[] {"FBOX", 3L});
    when(jpa.resumirContagens()).thenReturn(contagens);
    when(jpa.contarPorMaquina()).thenReturn(porMaquina);
    final var esperado =
        new com.rgm.api.core.domain.ports.repositories.ResumoModelos(
            7,
            5,
            2,
            2,
            List.of(
                new com.rgm.api.core.domain.ports.repositories.QuantidadePorMaquina("DISA", 4),
                new com.rgm.api.core.domain.ports.repositories.QuantidadePorMaquina("FBOX", 3)));

    // Act
    final var resumo = adapter.resumir();

    // Assert
    assertEquals(esperado, resumo);
    verify(jpa, org.mockito.Mockito.times(1)).resumirContagens();
    verify(jpa, org.mockito.Mockito.times(1)).contarPorMaquina();
    org.mockito.Mockito.verifyNoMoreInteractions(jpa);
  }

  // ── catálogo ─────────────────────────────────────────────────────────────

  private static final FiltrosDeModelos FILTROS =
      new FiltrosDeModelos(true, "MD", "FBOX", true, "desc", "RESINA", "100%_a");

  @Test
  void shouldChooseTheQueryByOrderAndEscapeTheSearchWildcards() {
    // Arrange
    when(jpa.catalogoPorCodigo(any(), any(), any(), anyBoolean(), any(), any(), any(), any()))
        .thenReturn(new PageImpl<>(List.of(criarJpaEntity()), PageRequest.of(1, 20), 41));
    when(jpa.catalogoPorCriacao(any(), any(), any(), anyBoolean(), any(), any(), any(), any()))
        .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

    // Act
    final PageResult<Modelo> porCodigo =
        adapter.buscarCatalogo(FILTROS, OrdemDeModelos.CODIGO, 1, 20);
    adapter.buscarCatalogo(FILTROS, OrdemDeModelos.CRIACAO, 0, 20);

    // Assert
    verify(jpa)
        .catalogoPorCodigo(
            true, "MD", "FBOX", true, "desc", "RESINA", "100\\%\\_a", PageRequest.of(1, 20));
    verify(jpa)
        .catalogoPorCriacao(
            true, "MD", "FBOX", true, "desc", "RESINA", "100\\%\\_a", PageRequest.of(0, 20));
    assertEquals(1, porCodigo.content().size());
    assertEquals(41, porCodigo.totalElements());
    assertEquals(3, porCodigo.totalPages());
    assertEquals(1, porCodigo.page());
  }

  @Test
  void shouldPassANullSearchThroughWhenThereIsNone() {
    // Arrange
    when(jpa.catalogoPorCodigo(any(), any(), any(), anyBoolean(), any(), any(), any(), any()))
        .thenReturn(new PageImpl<>(List.of()));

    // Act
    adapter.buscarCatalogo(
        new FiltrosDeModelos(null, null, null, false, null, null, null),
        OrdemDeModelos.CODIGO,
        0,
        20);

    // Assert
    verify(jpa).catalogoPorCodigo(any(), any(), any(), anyBoolean(), any(), any(), isNull(), any());
  }

  @Test
  void shouldCountEachGroupWithoutItsOwnFilter() {
    // Arrange
    when(jpa.contarNoCatalogo(any(), any(), any(), anyBoolean(), any(), any(), any()))
        .thenReturn(2L);
    when(jpa.contarNoCatalogoPorTipo(any(), any(), any(), anyBoolean(), any(), any(), any()))
        .thenReturn(List.<Object[]>of(new Object[] {"RESINA", 3L}, new Object[] {"COQUILHA", 1L}));
    when(jpa.contarNoCatalogoPorMaquina(any(), any(), any(), anyBoolean(), any(), any(), any()))
        .thenReturn(List.<Object[]>of(new Object[] {"FBOX", 4L}));

    // Act
    final ContagensDeModelos c = adapter.contar(FILTROS);

    // Assert
    assertEquals(2, c.total());
    assertEquals(
        List.of(
            new ContagensDeModelos.Quantidade("RESINA", 3),
            new ContagensDeModelos.Quantidade("COQUILHA", 1)),
        c.porTipo());
    assertEquals(List.of(new ContagensDeModelos.Quantidade("FBOX", 4)), c.porMaquina());
    verify(jpa).contarNoCatalogo(true, "MD", "FBOX", true, "desc", "RESINA", "100\\%\\_a");
    verify(jpa).contarNoCatalogoPorTipo(true, "MD", "FBOX", true, "desc", null, "100\\%\\_a");
    verify(jpa).contarNoCatalogoPorMaquina(true, "MD", null, false, "desc", "RESINA", "100\\%\\_a");
  }

  @Test
  void shouldGiveOnePlusTheModelsThatComeBefore() {
    // Arrange
    final ModeloJpaEntity alvo = criarJpaEntity();
    when(jpa.contarNoCatalogo(any(), any(), any(), anyBoolean(), any(), any(), any()))
        .thenReturn(45L);
    when(jpa.contarModelo(any(), any(), any(), anyBoolean(), any(), any(), any(), eq(alvo.getId())))
        .thenReturn(1L);
    when(jpa.findById(alvo.getId())).thenReturn(Optional.of(alvo));
    when(jpa.contarAntesDe(
            any(),
            any(),
            any(),
            anyBoolean(),
            any(),
            any(),
            any(),
            eq("MOD-001"),
            eq(1),
            eq(alvo.getId())))
        .thenReturn(32L);

    // Act
    final PosicaoDoModelo posicao = adapter.posicaoDe(alvo.getId(), FILTROS);

    // Assert
    assertEquals(33, posicao.posicao());
    assertEquals(45, posicao.total());
  }

  @Test
  void shouldGiveANullPositionAndNotCountTheOnesBeforeWhenTheModelIsOutsideTheFilter() {
    // Arrange
    final UUID id = UUID.randomUUID();
    when(jpa.contarNoCatalogo(any(), any(), any(), anyBoolean(), any(), any(), any()))
        .thenReturn(12L);
    when(jpa.contarModelo(any(), any(), any(), anyBoolean(), any(), any(), any(), eq(id)))
        .thenReturn(0L);

    // Act
    final PosicaoDoModelo posicao = adapter.posicaoDe(id, FILTROS);

    // Assert
    assertNull(posicao.posicao());
    assertEquals(12, posicao.total());
    verify(jpa, never())
        .contarAntesDe(
            any(), any(), any(), anyBoolean(), any(), any(), any(), any(), anyInt(), any());
  }
}
