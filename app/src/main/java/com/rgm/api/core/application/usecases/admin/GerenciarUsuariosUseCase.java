package com.rgm.api.core.application.usecases.admin;

import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.PasswordHasher;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** UC-13 (Usuarios): Cadastrar e gerenciar usuarios. Ator: Administrador. */
public final class GerenciarUsuariosUseCase {
  private static final Logger log = LoggerFactory.getLogger(GerenciarUsuariosUseCase.class);

  private final UsuarioRepository usuarioRepository;
  private final PasswordHasher passwordHasher;

  public GerenciarUsuariosUseCase(
      final UsuarioRepository usuarioRepository, final PasswordHasher passwordHasher) {
    this.usuarioRepository = usuarioRepository;
    this.passwordHasher = passwordHasher;
  }

  public record CriarInput(
      String nome, String email, String senha, PerfilUsuario perfil, UUID adminId) {}

  public record DesativarInput(UUID usuarioId, UUID adminId) {}

  public Usuario criar(final CriarInput input) {
    log.info("GerenciarUsuariosUseCase.criar iniciado");
    final Instant agora = Instant.now();
    validarPermissao(input.adminId());

    if (input.perfil() == PerfilUsuario.EXTERNO) {
      throw new BusinessRuleException("Use o caso de uso de cadastrar prestador externo");
    }

    if (usuarioRepository.existsByEmail(input.email())) {
      throw new BusinessRuleException("Email ja cadastrado");
    }

    final String senhaHash = passwordHasher.hash(input.senha());
    final Usuario usuario =
        Usuario.criarInterno(input.nome(), input.email(), senhaHash, input.perfil(), agora);

    return usuarioRepository.save(usuario);
  }

  public Usuario desativar(final DesativarInput input) {
    log.info("GerenciarUsuariosUseCase.desativar iniciado");
    final Instant agora = Instant.now();
    validarPermissao(input.adminId());

    final Usuario usuario =
        usuarioRepository
            .findById(input.usuarioId())
            .orElseThrow(() -> new ValidationException("Usuario nao encontrado"));

    return usuarioRepository.save(usuario.withAtivo(false, agora));
  }

  private void validarPermissao(final UUID adminId) {
    final Usuario admin =
        usuarioRepository
            .findById(adminId)
            .orElseThrow(() -> new ValidationException("Administrador nao encontrado"));

    if (!admin.getPerfil().podeGerenciarUsuariosEMaquinas()) {
      throw new NaoAutorizadoException("Somente ADMINISTRADOR pode gerenciar usuarios");
    }
  }
}
