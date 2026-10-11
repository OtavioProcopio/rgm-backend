package com.rgm.api.core.application.usecases.auth;

import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.AccessTokenIssuer;
import com.rgm.api.core.domain.ports.services.PasswordHasher;
import com.rgm.api.core.domain.validation.PoliticaSenha;
import java.time.Instant;
import java.util.UUID;

/** ADM-001/ADMIN-002: Alterar a propria senha. Ator: Qualquer usuario logado. */
public final class AlterarSenhaPropriaUseCase {

  private final UsuarioRepository usuarioRepository;
  private final PasswordHasher passwordHasher;
  private final AccessTokenIssuer tokenIssuer;

  public AlterarSenhaPropriaUseCase(
      final UsuarioRepository usuarioRepository,
      final PasswordHasher passwordHasher,
      final AccessTokenIssuer tokenIssuer) {
    this.usuarioRepository = usuarioRepository;
    this.passwordHasher = passwordHasher;
    this.tokenIssuer = tokenIssuer;
  }

  public record Input(UUID usuarioId, String senhaAtual, String novaSenha) {}

  /** As credenciais novas mantem aberta a sessao que trocou a senha; as antigas deixam de valer. */
  public record Output(Usuario usuario, String token, String refreshToken) {}

  public Output execute(final Input input) {
    final Instant agora = Instant.now();

    if (input.senhaAtual() == null || input.senhaAtual().isBlank()) {
      throw new BusinessRuleException("Senha atual e obrigatoria");
    }
    PoliticaSenha.validar(input.novaSenha());

    final Usuario usuario =
        usuarioRepository
            .findById(input.usuarioId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));

    if (usuario.getPerfil() == PerfilUsuario.EXTERNO) {
      throw new BusinessRuleException("Prestador externo nao possui senha");
    }

    if (!passwordHasher.matches(input.senhaAtual(), usuario.getSenhaHash())) {
      throw new BusinessRuleException("Senha atual incorreta");
    }

    final String novaSenhaHash = passwordHasher.hash(input.novaSenha());
    final Usuario salvo = usuarioRepository.save(usuario.withSenha(novaSenhaHash, agora));
    return new Output(salvo, tokenIssuer.issue(salvo), tokenIssuer.issueRefreshToken(salvo));
  }
}
