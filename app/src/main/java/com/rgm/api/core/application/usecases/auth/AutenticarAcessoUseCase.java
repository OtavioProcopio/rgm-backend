package com.rgm.api.core.application.usecases.auth;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.AccessTokenIssuer;
import com.rgm.api.core.domain.ports.services.CredencialToken;

/**
 * Identifica quem apresenta um token de acesso. Recusa token invalido, de usuario inexistente ou
 * inativo, e token emitido antes da ultima troca de senha. Devolve o usuario como esta agora, com o
 * perfil atual.
 */
public final class AutenticarAcessoUseCase {

  private final UsuarioRepository usuarioRepository;
  private final AccessTokenIssuer tokenIssuer;

  public AutenticarAcessoUseCase(
      final UsuarioRepository usuarioRepository, final AccessTokenIssuer tokenIssuer) {
    this.usuarioRepository = usuarioRepository;
    this.tokenIssuer = tokenIssuer;
  }

  public Usuario execute(final String accessToken) {
    final CredencialToken credencial;
    try {
      credencial = tokenIssuer.validateAccessToken(accessToken);
    } catch (final RuntimeException e) {
      throw new NaoAutorizadoException("Token invalido ou expirado");
    }

    final Usuario usuario =
        usuarioRepository
            .findById(credencial.usuarioId())
            .orElseThrow(() -> new NaoAutorizadoException("Usuario nao encontrado"));

    if (!usuario.isAtivo()) {
      throw new NaoAutorizadoException("Usuario inativo");
    }

    if (credencial.versaoCredencial() != usuario.getVersaoCredencial()) {
      throw new NaoAutorizadoException("Credencial emitida antes da troca de senha");
    }

    return usuario;
  }
}
