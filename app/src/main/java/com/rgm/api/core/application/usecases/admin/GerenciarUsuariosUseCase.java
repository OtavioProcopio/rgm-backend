package com.rgm.api.core.application.usecases.admin;

import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.PasswordHasher;
import java.time.Instant;
import java.util.UUID;

/** UC-13 (Usuarios): Cadastrar e gerenciar usuarios. Ator: Administrador. */
public final class GerenciarUsuariosUseCase {

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

  public record AtivarInput(UUID usuarioId, UUID adminId) {}

  public record EditarInput(UUID usuarioId, String nome, String email, UUID adminId) {}

  public record RedefinirSenhaInput(UUID usuarioId, String novaSenha, UUID adminId) {}

  public record AlterarPerfilInput(UUID usuarioId, PerfilUsuario novoPerfil, UUID adminId) {}

  public Usuario criar(final CriarInput input) {
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
    final Instant agora = Instant.now();
    validarPermissao(input.adminId());

    if (input.usuarioId().equals(input.adminId())) {
      throw new BusinessRuleException("Nao e possivel desativar a propria conta de administrador");
    }

    final Usuario usuario =
        usuarioRepository
            .findById(input.usuarioId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));

    return usuarioRepository.save(usuario.withAtivo(false, agora));
  }

  public Usuario ativar(final AtivarInput input) {
    final Instant agora = Instant.now();
    validarPermissao(input.adminId());

    final Usuario usuario =
        usuarioRepository
            .findById(input.usuarioId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));

    return usuarioRepository.save(usuario.withAtivo(true, agora));
  }

  public Usuario editar(final EditarInput input) {
    final Instant agora = Instant.now();
    validarPermissao(input.adminId());

    final Usuario usuario =
        usuarioRepository
            .findById(input.usuarioId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));

    if (usuario.getPerfil() == PerfilUsuario.EXTERNO) {
      throw new BusinessRuleException("Nao e possivel editar usuario EXTERNO por este caso de uso");
    }

    if (usuarioRepository.existsByEmailAndIdNot(input.email(), input.usuarioId())) {
      throw new BusinessRuleException("Email ja cadastrado");
    }

    return usuarioRepository.save(usuario.editar(input.nome(), input.email(), agora));
  }

  public Usuario redefinirSenha(final RedefinirSenhaInput input) {
    final Instant agora = Instant.now();
    validarPermissao(input.adminId());

    if (input.novaSenha() == null || input.novaSenha().isBlank()) {
      throw new BusinessRuleException("Nova senha e obrigatoria");
    }

    final Usuario usuario =
        usuarioRepository
            .findById(input.usuarioId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));

    if (usuario.getPerfil() == PerfilUsuario.EXTERNO) {
      throw new BusinessRuleException("Nao e possivel redefinir senha de usuario EXTERNO");
    }

    final String novaSenhaHash = passwordHasher.hash(input.novaSenha());
    return usuarioRepository.save(usuario.withSenha(novaSenhaHash, agora));
  }

  public Usuario alterarPerfil(final AlterarPerfilInput input) {
    final Instant agora = Instant.now();
    validarPermissao(input.adminId());

    if (input.usuarioId().equals(input.adminId())
        && input.novoPerfil() != PerfilUsuario.ADMINISTRADOR) {
      throw new BusinessRuleException("Nao e possivel alterar o proprio perfil de administrador");
    }

    final Usuario usuario =
        usuarioRepository
            .findById(input.usuarioId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));

    if (usuario.getPerfil() == PerfilUsuario.EXTERNO
        && input.novoPerfil() != PerfilUsuario.EXTERNO) {
      throw new BusinessRuleException("Nao e possivel alterar o perfil de um usuario EXTERNO");
    }
    if (input.novoPerfil() == PerfilUsuario.EXTERNO
        && usuario.getPerfil() != PerfilUsuario.EXTERNO) {
      throw new BusinessRuleException("Nao e possivel transformar um usuario interno em EXTERNO");
    }

    return usuarioRepository.save(usuario.alterarPerfil(input.novoPerfil(), agora));
  }

  private void validarPermissao(final UUID adminId) {
    final Usuario admin =
        usuarioRepository
            .findById(adminId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Administrador nao encontrado"));

    if (!admin.getPerfil().podeGerenciarUsuariosEMaquinas()) {
      throw new NaoAutorizadoException("Somente ADMINISTRADOR pode gerenciar usuarios");
    }
  }
}
