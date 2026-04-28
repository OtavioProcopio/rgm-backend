package com.rgm.api.core.application.usecases.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.AccessTokenIssuer;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RefreshTokenUseCaseTest {

  private UsuarioRepository usuarioRepository;
  private AccessTokenIssuer tokenIssuer;
  private RefreshTokenUseCase useCase;

  @BeforeEach
  void setUp() {
    usuarioRepository = mock(UsuarioRepository.class);
    tokenIssuer = mock(AccessTokenIssuer.class);
    useCase = new RefreshTokenUseCase(usuarioRepository, tokenIssuer);
  }

  @Test
  void deveRefreshComSucesso() {
    final UUID userId = UUID.randomUUID();
    final Instant agora = Instant.now();
    final Usuario usuario =
        new Usuario(userId, "Joao", "j@t.com", "hash", PerfilUsuario.OPERADOR, true, agora, agora);

    when(tokenIssuer.validateRefreshToken("old-refresh")).thenReturn(userId);
    when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
    when(tokenIssuer.issue(usuario)).thenReturn("new-access");
    when(tokenIssuer.issueRefreshToken(usuario)).thenReturn("new-refresh");

    final RefreshTokenUseCase.Output output =
        useCase.execute(new RefreshTokenUseCase.Input("old-refresh"));

    assertEquals("new-access", output.token());
    assertEquals("new-refresh", output.refreshToken());
  }

  @Test
  void deveFalharComTokenInvalido() {
    when(tokenIssuer.validateRefreshToken("bad")).thenThrow(new RuntimeException("invalido"));

    assertThrows(
        NaoAutorizadoException.class, () -> useCase.execute(new RefreshTokenUseCase.Input("bad")));
  }

  @Test
  void deveFalharComUsuarioInativo() {
    final UUID userId = UUID.randomUUID();
    final Instant agora = Instant.now();
    final Usuario inativo =
        new Usuario(userId, "Joao", "j@t.com", "hash", PerfilUsuario.OPERADOR, false, agora, agora);

    when(tokenIssuer.validateRefreshToken("token")).thenReturn(userId);
    when(usuarioRepository.findById(userId)).thenReturn(Optional.of(inativo));

    assertThrows(
        NaoAutorizadoException.class,
        () -> useCase.execute(new RefreshTokenUseCase.Input("token")));
  }
}
