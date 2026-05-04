package com.rgm.api.core.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UsuarioTest {

  private static final Instant AGORA = Instant.now();

  @Test
  void deveCriarUsuarioInterno() {
    final Usuario usuario =
        Usuario.criarInterno("Admin", "admin@rgm.com", "hash", PerfilUsuario.ADMINISTRADOR, AGORA);

    assertNotNull(usuario.getId());
    assertEquals("Admin", usuario.getNome());
    assertEquals("admin@rgm.com", usuario.getEmail());
    assertEquals(PerfilUsuario.ADMINISTRADOR, usuario.getPerfil());
    assertTrue(usuario.isAtivo());
  }

  @Test
  void deveCriarPrestadorExterno() {
    final Usuario externo = Usuario.criarExterno("Prestador", AGORA);

    assertNotNull(externo.getId());
    assertEquals("Prestador", externo.getNome());
    assertNull(externo.getEmail());
    assertNull(externo.getSenhaHash());
    assertEquals(PerfilUsuario.EXTERNO, externo.getPerfil());
  }

  @Test
  void deveFalharCriarInternoComPerfilExterno() {
    assertThrows(
        ValidationException.class,
        () -> Usuario.criarInterno("Nome", "email@test.com", "hash", PerfilUsuario.EXTERNO, AGORA));
  }

  @Test
  void deveFalharCriarInternoSemNome() {
    assertThrows(
        NullPointerException.class,
        () -> Usuario.criarInterno(null, "email@test.com", "hash", PerfilUsuario.OPERADOR, AGORA));
  }

  @Test
  void deveFalharCriarInternoSemSenha() {
    assertThrows(
        ValidationException.class,
        () -> Usuario.criarInterno("Nome", "email@test.com", null, PerfilUsuario.OPERADOR, AGORA));
  }

  @Test
  void deveDesativarUsuario() {
    final Usuario usuario =
        Usuario.criarInterno("Admin", "admin@rgm.com", "hash", PerfilUsuario.GESTOR, AGORA);
    final Usuario desativado = usuario.withAtivo(false, AGORA);

    assertFalse(desativado.isAtivo());
    assertEquals(usuario.getId(), desativado.getId());
  }
}
