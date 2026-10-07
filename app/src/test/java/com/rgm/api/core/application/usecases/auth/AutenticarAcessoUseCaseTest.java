package com.rgm.api.core.application.usecases.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.AccessTokenIssuer;
import com.rgm.api.core.domain.ports.services.CredencialToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AutenticarAcessoUseCaseTest {

  private static final Instant AGORA = Instant.parse("2026-10-07T12:00:00Z");
  private static final String TOKEN = "token-de-acesso";
  private static final int VERSAO = 3;

  @Mock private UsuarioRepository usuarioRepository;
  @Mock private AccessTokenIssuer tokenIssuer;
  @InjectMocks private AutenticarAcessoUseCase useCase;

  private static Usuario usuario(final boolean ativo) {
    return new Usuario(
        UUID.randomUUID(),
        "Joao",
        "joao@rgm.test",
        "hash",
        PerfilUsuario.OPERADOR,
        ativo,
        AGORA,
        AGORA,
        VERSAO);
  }

  @Test
  void shouldReturnCurrentUserWhenTokenCarriesCurrentCredentialVersion() {
    // Arrange
    final Usuario usuario = usuario(true);
    when(tokenIssuer.validateAccessToken(TOKEN))
        .thenReturn(new CredencialToken(usuario.getId(), VERSAO));
    when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));

    // Act
    final Usuario autenticado = useCase.execute(TOKEN);

    // Assert
    assertSame(usuario, autenticado);
    verify(tokenIssuer, times(1)).validateAccessToken(TOKEN);
    verify(usuarioRepository, times(1)).findById(usuario.getId());
    verifyNoMoreInteractions(tokenIssuer, usuarioRepository);
  }

  @Test
  void shouldRejectWhenTokenIsInvalid() {
    // Arrange
    when(tokenIssuer.validateAccessToken(TOKEN)).thenThrow(new IllegalArgumentException("ruim"));

    // Act
    final NaoAutorizadoException erro =
        assertThrows(NaoAutorizadoException.class, () -> useCase.execute(TOKEN));

    // Assert
    assertEquals("Token invalido ou expirado", erro.getMessage());
    verify(tokenIssuer, times(1)).validateAccessToken(TOKEN);
    verifyNoMoreInteractions(tokenIssuer, usuarioRepository);
  }

  @Test
  void shouldRejectWhenUserDoesNotExist() {
    // Arrange
    final UUID usuarioId = UUID.randomUUID();
    when(tokenIssuer.validateAccessToken(TOKEN)).thenReturn(new CredencialToken(usuarioId, 0));
    when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

    // Act
    final NaoAutorizadoException erro =
        assertThrows(NaoAutorizadoException.class, () -> useCase.execute(TOKEN));

    // Assert
    assertEquals("Usuario nao encontrado", erro.getMessage());
    verify(tokenIssuer, times(1)).validateAccessToken(TOKEN);
    verify(usuarioRepository, times(1)).findById(usuarioId);
    verifyNoMoreInteractions(tokenIssuer, usuarioRepository);
  }

  @Test
  void shouldRejectWhenUserIsInactive() {
    // Arrange
    final Usuario inativo = usuario(false);
    when(tokenIssuer.validateAccessToken(TOKEN))
        .thenReturn(new CredencialToken(inativo.getId(), VERSAO));
    when(usuarioRepository.findById(inativo.getId())).thenReturn(Optional.of(inativo));

    // Act
    final NaoAutorizadoException erro =
        assertThrows(NaoAutorizadoException.class, () -> useCase.execute(TOKEN));

    // Assert
    assertEquals("Usuario inativo", erro.getMessage());
    verify(tokenIssuer, times(1)).validateAccessToken(TOKEN);
    verify(usuarioRepository, times(1)).findById(inativo.getId());
    verifyNoMoreInteractions(tokenIssuer, usuarioRepository);
  }

  @Test
  void shouldRejectWhenTokenWasIssuedBeforePasswordChange() {
    // Arrange
    final Usuario usuario = usuario(true);
    when(tokenIssuer.validateAccessToken(TOKEN))
        .thenReturn(new CredencialToken(usuario.getId(), VERSAO - 1));
    when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));

    // Act
    final NaoAutorizadoException erro =
        assertThrows(NaoAutorizadoException.class, () -> useCase.execute(TOKEN));

    // Assert
    assertEquals("Credencial emitida antes da troca de senha", erro.getMessage());
    verify(tokenIssuer, times(1)).validateAccessToken(TOKEN);
    verify(usuarioRepository, times(1)).findById(usuario.getId());
    verifyNoMoreInteractions(tokenIssuer, usuarioRepository);
  }
}
