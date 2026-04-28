package com.rgm.api.core.application.usecases.admin;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.UUID;

/** UC-11: Cadastrar prestador externo (perfil EXTERNO). */
public final class CadastrarPrestadorExternoUseCase {

  private final UsuarioRepository usuarioRepository;

  public CadastrarPrestadorExternoUseCase(final UsuarioRepository usuarioRepository) {
    this.usuarioRepository = usuarioRepository;
  }

  public record Input(String nome, UUID adminId) {}

  public Usuario execute(final Input input) {
    final Instant agora = Instant.now();

    final Usuario admin =
        usuarioRepository
            .findById(input.adminId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Administrador nao encontrado"));

    if (!admin.getPerfil().podeGerenciarUsuariosEMaquinas()) {
      throw new NaoAutorizadoException("Somente ADMINISTRADOR pode cadastrar prestador externo");
    }

    final Usuario prestador = Usuario.criarExterno(input.nome(), agora);
    return usuarioRepository.save(prestador);
  }
}
