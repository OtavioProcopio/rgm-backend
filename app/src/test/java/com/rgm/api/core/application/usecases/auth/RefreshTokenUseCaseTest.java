package com.rgm.api.core.application.usecases.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.AccessTokenIssuer;
import com.rgm.api.core.domain.ports.services.CredencialToken;
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

    when(tokenIssuer.validateRefreshToken("old-refresh"))
        .thenReturn(new CredencialToken(userId, 0));
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

    when(tokenIssuer.validateRefreshToken("token")).thenReturn(new CredencialToken(userId, 0));
    when(usuarioRepository.findById(userId)).thenReturn(Optional.of(inativo));

    assertThrows(
        NaoAutorizadoException.class,
        () -> useCase.execute(new RefreshTokenUseCase.Input("token")));
  }

  private static Usuario comVersao(final UUID id, final int versao) {
    final Instant agora = Instant.now();
    return new Usuario(
        id, "Joao", "j@t.com", "hash", PerfilUsuario.OPERADOR, true, agora, agora, versao);
  }

  @Test
  void shouldRejectRefreshTokenWhenIssuedBeforePasswordChange() {
    // Arrange
    final UUID userId = UUID.randomUUID();
    final Usuario usuario = comVersao(userId, 2);
    when(tokenIssuer.validateRefreshToken("antigo"))
        .thenReturn(new CredencialToken(userId, usuario.getVersaoCredencial() - 1));
    when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));

    // Act
    final NaoAutorizadoException erro =
        assertThrows(
            NaoAutorizadoException.class,
            () -> useCase.execute(new RefreshTokenUseCase.Input("antigo")));

    // Assert
    assertEquals("Refresh token invalido ou expirado", erro.getMessage());
    verify(tokenIssuer, times(1)).validateRefreshToken("antigo");
    verify(usuarioRepository, times(1)).findById(userId);
    verifyNoMoreInteractions(tokenIssuer, usuarioRepository);
  }

  @Test
  void shouldRenewWhenRefreshTokenCarriesCurrentCredentialVersion() {
    // Arrange
    final UUID userId = UUID.randomUUID();
    final Usuario usuario = comVersao(userId, 2);
    when(tokenIssuer.validateRefreshToken("atual"))
        .thenReturn(new CredencialToken(userId, usuario.getVersaoCredencial()));
    when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
    when(tokenIssuer.issue(usuario)).thenReturn("novo-acesso");
    when(tokenIssuer.issueRefreshToken(usuario)).thenReturn("nova-renovacao");

    // Act
    final RefreshTokenUseCase.Output output =
        useCase.execute(new RefreshTokenUseCase.Input("atual"));

    // Assert
    assertEquals(new RefreshTokenUseCase.Output("novo-acesso", "nova-renovacao"), output);
    verify(tokenIssuer, times(1)).validateRefreshToken("atual");
    verify(usuarioRepository, times(1)).findById(userId);
    verify(tokenIssuer, times(1)).issue(usuario);
    verify(tokenIssuer, times(1)).issueRefreshToken(usuario);
    verifyNoMoreInteractions(tokenIssuer, usuarioRepository);
  }
}
