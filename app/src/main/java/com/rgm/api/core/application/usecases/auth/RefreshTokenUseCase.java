package com.rgm.api.core.application.usecases.auth;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.AccessTokenIssuer;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Renovar access token usando um refresh token valido. */
public final class RefreshTokenUseCase {
  private static final Logger log = LoggerFactory.getLogger(RefreshTokenUseCase.class);

  private final UsuarioRepository usuarioRepository;
  private final AccessTokenIssuer tokenIssuer;

  public RefreshTokenUseCase(
      final UsuarioRepository usuarioRepository, final AccessTokenIssuer tokenIssuer) {
    this.usuarioRepository = usuarioRepository;
    this.tokenIssuer = tokenIssuer;
  }

  public record Input(String refreshToken) {}

  public record Output(String token, String refreshToken) {}

  public Output execute(final Input input) {
    log.info("RefreshTokenUseCase.execute iniciado");
    final UUID usuarioId;
    try {
      usuarioId = tokenIssuer.validateRefreshToken(input.refreshToken());
    } catch (final Exception e) {
      throw new NaoAutorizadoException("Refresh token invalido ou expirado");
    }

    final Usuario usuario =
        usuarioRepository
            .findById(usuarioId)
            .orElseThrow(() -> new NaoAutorizadoException("Usuario nao encontrado"));

    if (!usuario.isAtivo()) {
      throw new NaoAutorizadoException("Usuario inativo");
    }

    final String newAccessToken = tokenIssuer.issue(usuario);
    final String newRefreshToken = tokenIssuer.issueRefreshToken(usuario);
    return new Output(newAccessToken, newRefreshToken);
  }
}
