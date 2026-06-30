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

import com.rgm.api.adapter.out.persistence.entity.UsuarioJpaEntity;
import com.rgm.api.adapter.out.persistence.repository.UsuarioJpaRepository;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
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

class UsuarioRepositoryAdapterTest {

  private UsuarioJpaRepository jpa;
  private UsuarioRepositoryAdapter adapter;

  @BeforeEach
  void setUp() {
    jpa = mock(UsuarioJpaRepository.class);
    adapter = new UsuarioRepositoryAdapter(jpa);
  }

  private UsuarioJpaEntity criarEntity() {
    final UsuarioJpaEntity e = new UsuarioJpaEntity();
    e.setId(UUID.randomUUID());
    e.setNome("Ana");
    e.setEmail("ana@rgm.com");
    e.setSenhaHash("hash");
    e.setPerfil(PerfilUsuario.OPERADOR);
    e.setAtivo(true);
    e.setCriadoEm(Instant.now());
    e.setAtualizadoEm(Instant.now());
    return e;
  }

  private Usuario criarDomain() {
    return new Usuario(
        UUID.randomUUID(),
        "Ana",
        "ana@rgm.com",
        "hash",
        PerfilUsuario.OPERADOR,
        true,
        Instant.now(),
        Instant.now());
  }

  @Test
  void findById_quandoExistir_retornaUsuario() {
    final UUID id = UUID.randomUUID();
    final UsuarioJpaEntity e = criarEntity();
    e.setId(id);
    when(jpa.findById(id)).thenReturn(Optional.of(e));

    final Optional<Usuario> result = adapter.findById(id);

    assertTrue(result.isPresent());
    assertEquals("ana@rgm.com", result.get().getEmail());
    verify(jpa).findById(id);
  }

  @Test
  void findById_quandoNaoExistir_retornaVazio() {
    final UUID id = UUID.randomUUID();
    when(jpa.findById(id)).thenReturn(Optional.empty());

    assertFalse(adapter.findById(id).isPresent());
  }

  @Test
  void findByEmail_quandoExistir_retornaUsuario() {
    final UsuarioJpaEntity e = criarEntity();
    when(jpa.findByEmail("ana@rgm.com")).thenReturn(Optional.of(e));

    final Optional<Usuario> result = adapter.findByEmail("ana@rgm.com");

    assertTrue(result.isPresent());
    assertEquals("Ana", result.get().getNome());
  }

  @Test
  void findByEmail_quandoNaoExistir_retornaVazio() {
    when(jpa.findByEmail("nao@existe.com")).thenReturn(Optional.empty());

    assertFalse(adapter.findByEmail("nao@existe.com").isPresent());
  }

  @Test
  void findAllByIdIn_retornaListaMapped() {
    final UUID id = UUID.randomUUID();
    final UsuarioJpaEntity e = criarEntity();
    e.setId(id);
    when(jpa.findAllByIdIn(List.of(id))).thenReturn(List.of(e));

    final List<Usuario> result = adapter.findAllByIdIn(List.of(id));

    assertEquals(1, result.size());
    assertEquals("ana@rgm.com", result.get(0).getEmail());
  }

  @Test
  void save_persisteERetorna() {
    final Usuario domain = criarDomain();
    final UsuarioJpaEntity e = criarEntity();
    when(jpa.save(any(UsuarioJpaEntity.class))).thenReturn(e);

    final Usuario result = adapter.save(domain);

    assertNotNull(result);
    verify(jpa).save(any(UsuarioJpaEntity.class));
  }

  @Test
  void deleteById_delegaAoJpa() {
    final UUID id = UUID.randomUUID();
    doNothing().when(jpa).deleteById(id);

    adapter.deleteById(id);

    verify(jpa).deleteById(id);
  }

  @Test
  void existsByEmail_delegaAoJpa() {
    when(jpa.existsByEmail("ana@rgm.com")).thenReturn(true);

    assertTrue(adapter.existsByEmail("ana@rgm.com"));
  }

  @Test
  void existsByEmailAndIdNot_delegaAoJpa() {
    final UUID id = UUID.randomUUID();
    when(jpa.existsByEmailAndIdNot("ana@rgm.com", id)).thenReturn(false);

    assertFalse(adapter.existsByEmailAndIdNot("ana@rgm.com", id));
  }

  @Test
  void count_delegaAoJpa() {
    when(jpa.count()).thenReturn(7L);

    assertEquals(7L, adapter.count());
  }

  @Test
  void findAll_retornaPaginado() {
    final UsuarioJpaEntity e = criarEntity();
    final Page<UsuarioJpaEntity> page = new PageImpl<>(List.of(e), PageRequest.of(0, 10), 1);
    when(jpa.findAll(any(PageRequest.class))).thenReturn(page);

    final PageResult<Usuario> result = adapter.findAll(0, 10);

    assertEquals(1, result.content().size());
    assertEquals(1L, result.totalElements());
  }

  @Test
  void findByFilters_retornaPaginado() {
    final UsuarioJpaEntity e = criarEntity();
    final Page<UsuarioJpaEntity> page = new PageImpl<>(List.of(e), PageRequest.of(0, 10), 1);
    when(jpa.findByFilters(any(), any(), any(PageRequest.class))).thenReturn(page);

    final PageResult<Usuario> result = adapter.findByFilters(PerfilUsuario.OPERADOR, true, 0, 10);

    assertEquals(1, result.content().size());
  }
}
