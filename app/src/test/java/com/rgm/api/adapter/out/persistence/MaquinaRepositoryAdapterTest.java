package com.rgm.api.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rgm.api.adapter.out.persistence.entity.MaquinaJpaEntity;
import com.rgm.api.adapter.out.persistence.repository.MaquinaJpaRepository;
import com.rgm.api.core.domain.model.aggregates.Maquina;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class MaquinaRepositoryAdapterTest {

  private MaquinaJpaRepository jpa;
  private MaquinaRepositoryAdapter adapter;

  @BeforeEach
  void setUp() {
    jpa = mock(MaquinaJpaRepository.class);
    adapter = new MaquinaRepositoryAdapter(jpa);
  }

  private MaquinaJpaEntity entity(final String nome, final boolean ativo) {
    final Instant agora = Instant.now();
    return new MaquinaJpaEntity(UUID.randomUUID(), nome, ativo, agora, agora);
  }

  @Test
  void savePersisteERetorna() {
    final Instant agora = Instant.now();
    final Maquina domain = new Maquina(UUID.randomUUID(), "VICK", true, agora, agora);
    when(jpa.save(any(MaquinaJpaEntity.class))).thenReturn(entity("VICK", true));

    final Maquina result = adapter.save(domain);

    assertEquals("VICK", result.getNome());
  }

  @Test
  void findByIdMapeia() {
    final UUID id = UUID.randomUUID();
    when(jpa.findById(id)).thenReturn(Optional.of(entity("FBO", true)));

    final Optional<Maquina> result = adapter.findById(id);

    assertTrue(result.isPresent());
    assertEquals("FBO", result.get().getNome());
  }

  @Test
  void findAllOrdenaPorNome() {
    when(jpa.findAll(any(Sort.class)))
        .thenReturn(List.of(entity("FBO", true), entity("VICK", false)));

    final List<Maquina> result = adapter.findAll();

    assertEquals(2, result.size());
  }

  @Test
  void existsByNomeDelega() {
    when(jpa.existsByNomeIgnoreCase("VICK")).thenReturn(true);
    assertTrue(adapter.existsByNome("VICK"));
  }

  @Test
  void existsByNomeAndAtivoTrueDelega() {
    when(jpa.existsByNomeIgnoreCaseAndAtivoTrue("VICK")).thenReturn(false);
    assertFalse(adapter.existsByNomeAndAtivoTrue("VICK"));
  }
}
