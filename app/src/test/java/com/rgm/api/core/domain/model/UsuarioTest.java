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

  @Test
  void shouldStartAtCredentialVersionZeroWhenCreated() {
    // Arrange
    final int esperado = 0;

    // Act
    final Usuario usuario =
        Usuario.criarInterno("Admin", "admin@rgm.com", "hash", PerfilUsuario.GESTOR, AGORA);

    // Assert
    assertEquals(esperado, usuario.getVersaoCredencial());
  }

  @Test
  void shouldIncrementCredentialVersionWhenPasswordChanges() {
    // Arrange
    final Usuario usuario = comVersao(3);

    // Act
    final Usuario alterado = usuario.withSenha("novo-hash", AGORA);

    // Assert
    assertEquals(usuario.getVersaoCredencial() + 1, alterado.getVersaoCredencial());
  }

  @Test
  void shouldKeepCredentialVersionWhenProfileChanges() {
    // Arrange
    final Usuario usuario = comVersao(3);

    // Act
    final Usuario alterado = usuario.alterarPerfil(PerfilUsuario.OPERADOR, AGORA);

    // Assert
    assertEquals(usuario.getVersaoCredencial(), alterado.getVersaoCredencial());
  }

  @Test
  void shouldKeepCredentialVersionWhenEdited() {
    // Arrange
    final Usuario usuario = comVersao(3);

    // Act
    final Usuario alterado = usuario.editar("Outro", "outro@rgm.com", AGORA);

    // Assert
    assertEquals(usuario.getVersaoCredencial(), alterado.getVersaoCredencial());
  }

  @Test
  void shouldKeepCredentialVersionWhenDeactivated() {
    // Arrange
    final Usuario usuario = comVersao(3);

    // Act
    final Usuario alterado = usuario.withAtivo(false, AGORA);

    // Assert
    assertEquals(usuario.getVersaoCredencial(), alterado.getVersaoCredencial());
  }

  @Test
  void shouldRejectNegativeCredentialVersion() {
    // Arrange
    final int versaoInvalida = -1;

    // Act
    final IllegalArgumentException erro =
        assertThrows(IllegalArgumentException.class, () -> comVersao(versaoInvalida));

    // Assert
    assertEquals("versaoCredencial deve ser >= 0", erro.getMessage());
  }

  private static Usuario comVersao(final int versao) {
    return new Usuario(
        java.util.UUID.randomUUID(),
        "Admin",
        "admin@rgm.com",
        "hash",
        PerfilUsuario.GESTOR,
        true,
        AGORA,
        AGORA,
        versao);
  }
}
