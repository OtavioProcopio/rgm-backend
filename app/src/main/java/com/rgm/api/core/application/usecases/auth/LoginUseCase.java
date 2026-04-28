package com.rgm.api.core.application.usecases.auth;

import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.AccessTokenIssuer;
import com.rgm.api.core.domain.ports.services.PasswordHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** UC-01: Logar no sistema. */
public final class LoginUseCase {
  private static final Logger log = LoggerFactory.getLogger(LoginUseCase.class);

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
    log.info("LoginUseCase.execute iniciado");
    final Usuario usuario =
        usuarioRepository
            .findByEmail(input.email())
            .orElseThrow(() -> new NaoAutorizadoException("Credenciais invalidas"));

    if (!usuario.getPerfil().fazLogin()) {
      throw new BusinessRuleException("Perfil EXTERNO nao faz login");
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
