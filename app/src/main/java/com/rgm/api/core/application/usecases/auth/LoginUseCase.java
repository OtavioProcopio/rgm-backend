package com.rgm.api.core.application.usecases.auth;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.AccessTokenIssuer;
import com.rgm.api.core.domain.ports.services.PasswordHasher;

/** UC-01: Logar no sistema. */
public final class LoginUseCase {

  private final UsuarioRepository usuarioRepository;
  private final PasswordHasher passwordHasher;
  private final AccessTokenIssuer tokenIssuer;

  public LoginUseCase(
      final UsuarioRepository usuarioRepository,
      final PasswordHasher passwordHasher,
      final AccessTokenIssuer tokenIssuer) {
    this.usuarioRepository = usuarioRepository;
    this.passwordHasher = passwordHasher;
    this.tokenIssuer = tokenIssuer;
  }

  public record Input(String email, String senha) {}

  public record Output(String token, String refreshToken, String nome, String perfil) {}

  public Output execute(final Input input) {
    final Usuario usuario =
        usuarioRepository
            .findByEmail(input.email())
            .orElseThrow(() -> new NaoAutorizadoException("Credenciais invalidas"));

    if (!usuario.getPerfil().fazLogin()) {
      throw new NaoAutorizadoException("Credenciais invalidas");
    }

    if (!usuario.isAtivo()) {
      throw new NaoAutorizadoException("Usuario inativo");
    }

    if (!passwordHasher.matches(input.senha(), usuario.getSenhaHash())) {
      throw new NaoAutorizadoException("Credenciais invalidas");
    }

    final String token = tokenIssuer.issue(usuario);
    final String refreshToken = tokenIssuer.issueRefreshToken(usuario);
    return new Output(token, refreshToken, usuario.getNome(), usuario.getPerfil().name());
  }
}
