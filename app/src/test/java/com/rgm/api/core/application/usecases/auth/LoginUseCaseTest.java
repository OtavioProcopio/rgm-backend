package com.rgm.api.core.application.usecases.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.AccessTokenIssuer;
import com.rgm.api.core.domain.ports.services.PasswordHasher;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginUseCaseTest {

  private UsuarioRepository usuarioRepository;
  private PasswordHasher passwordHasher;
  private AccessTokenIssuer tokenIssuer;
  private LoginUseCase useCase;

  @BeforeEach
  void setUp() {
    usuarioRepository = mock(UsuarioRepository.class);
    passwordHasher = mock(PasswordHasher.class);
    tokenIssuer = mock(AccessTokenIssuer.class);
    useCase = new LoginUseCase(usuarioRepository, passwordHasher, tokenIssuer);
  }

  @Test
  void deveLogarComSucesso() {
    final Instant agora = Instant.now();
    final Usuario usuario =
        new Usuario(
            UUID.randomUUID(),
            "Joao",
            "joao@test.com",
            "hashed",
            PerfilUsuario.OPERADOR,
            true,
            agora,
            agora);

    when(usuarioRepository.findByEmail("joao@test.com")).thenReturn(Optional.of(usuario));
    when(passwordHasher.matches("senha123", "hashed")).thenReturn(true);
    when(tokenIssuer.issue(usuario)).thenReturn("jwt-token");
    when(tokenIssuer.issueRefreshToken(usuario)).thenReturn("refresh-token");

    final LoginUseCase.Output output =
        useCase.execute(new LoginUseCase.Input("joao@test.com", "senha123"));

    assertEquals("jwt-token", output.token());
    assertEquals("refresh-token", output.refreshToken());
    assertEquals("Joao", output.nome());
    assertEquals("OPERADOR", output.perfil());
  }

  @Test
  void deveFalharComCredenciaisInvalidas() {
    when(usuarioRepository.findByEmail("nao@existe.com")).thenReturn(Optional.empty());

    assertThrows(
        NaoAutorizadoException.class,
        () -> useCase.execute(new LoginUseCase.Input("nao@existe.com", "senha")));
  }

  @Test
  void deveFalharComSenhaErrada() {
    final Instant agora = Instant.now();
    final Usuario usuario =
        new Usuario(
            UUID.randomUUID(),
            "Joao",
            "joao@test.com",
            "hashed",
            PerfilUsuario.OPERADOR,
            true,
            agora,
            agora);

    when(usuarioRepository.findByEmail("joao@test.com")).thenReturn(Optional.of(usuario));
    when(passwordHasher.matches("errada", "hashed")).thenReturn(false);

    assertThrows(
        NaoAutorizadoException.class,
        () -> useCase.execute(new LoginUseCase.Input("joao@test.com", "errada")));
  }

  @Test
  void deveFalharComUsuarioInativo() {
    final Instant agora = Instant.now();
    final Usuario usuario =
        new Usuario(
            UUID.randomUUID(),
            "Joao",
            "joao@test.com",
            "hashed",
            PerfilUsuario.OPERADOR,
            false,
            agora,
            agora);

    when(usuarioRepository.findByEmail("joao@test.com")).thenReturn(Optional.of(usuario));

    assertThrows(
        NaoAutorizadoException.class,
        () -> useCase.execute(new LoginUseCase.Input("joao@test.com", "senha")));
  }

  @Test
  void deveFalharComPerfilExterno() {
    final Instant agora = Instant.now();
    final Usuario externo =
        new Usuario(
            UUID.randomUUID(), "Externo", null, null, PerfilUsuario.EXTERNO, true, agora, agora);

    when(usuarioRepository.findByEmail(null)).thenReturn(Optional.of(externo));

    assertThrows(
        NaoAutorizadoException.class, () -> useCase.execute(new LoginUseCase.Input(null, "senha")));
  }

  @Test
  void shouldReturnUserIdWhenLoginSucceeds() {
    // Arrange
    final Instant agora = Instant.now();
    final UUID usuarioId = UUID.randomUUID();
    final Usuario usuario =
        new Usuario(
            usuarioId,
            "Joao",
            "joao@rgm.test",
            "hashed",
            PerfilUsuario.OPERADOR,
            true,
            agora,
            agora);
    when(usuarioRepository.findByEmail("joao@rgm.test")).thenReturn(Optional.of(usuario));
    when(passwordHasher.matches("senha-correta", "hashed")).thenReturn(true);
    when(tokenIssuer.issue(usuario)).thenReturn("jwt-token");
    when(tokenIssuer.issueRefreshToken(usuario)).thenReturn("refresh-token");

    // Act
    final LoginUseCase.Output output =
        useCase.execute(new LoginUseCase.Input("joao@rgm.test", "senha-correta"));

    // Assert
    assertEquals(
        new LoginUseCase.Output(usuarioId, "jwt-token", "refresh-token", "Joao", "OPERADOR"),
        output);
    verify(usuarioRepository, times(1)).findByEmail("joao@rgm.test");
    verify(passwordHasher, times(1)).matches("senha-correta", "hashed");
    verify(tokenIssuer, times(1)).issue(usuario);
    verify(tokenIssuer, times(1)).issueRefreshToken(usuario);
    verifyNoMoreInteractions(usuarioRepository, passwordHasher, tokenIssuer);
  }
}
