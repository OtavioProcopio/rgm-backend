package com.rgm.api.core.application.usecases.admin;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Maquina;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.ports.repositories.MaquinaRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.UUID;

/** UC-13 (Maquinas): Cadastrar e gerenciar maquinas. Ator: Administrador. */
public final class GerenciarMaquinasUseCase {

  private final MaquinaRepository maquinaRepository;
  private final UsuarioRepository usuarioRepository;

  public GerenciarMaquinasUseCase(
      final MaquinaRepository maquinaRepository, final UsuarioRepository usuarioRepository) {
    this.maquinaRepository = maquinaRepository;
    this.usuarioRepository = usuarioRepository;
  }

  public record CriarInput(String nome, String codigo, String descricao, UUID adminId) {}

  public record EditarInput(
      UUID maquinaId, String nome, String codigo, String descricao, UUID adminId) {}

  public Maquina criar(final CriarInput input) {
    final Instant agora = Instant.now();
    validarPermissao(input.adminId());

    final Maquina maquina = Maquina.criar(input.nome(), input.codigo(), input.descricao(), agora);
    return maquinaRepository.save(maquina);
  }

  public Maquina editar(final EditarInput input) {
    final Instant agora = Instant.now();
    validarPermissao(input.adminId());

    final Maquina maquina =
        maquinaRepository
            .findById(input.maquinaId())
            .orElseThrow(() -> new ValidationException("Maquina nao encontrada"));

    final Maquina editada = maquina.editar(input.nome(), input.codigo(), input.descricao(), agora);

    return maquinaRepository.save(editada);
  }

  private void validarPermissao(final UUID adminId) {
    final Usuario admin =
        usuarioRepository
            .findById(adminId)
            .orElseThrow(() -> new ValidationException("Administrador nao encontrado"));

    if (!admin.getPerfil().podeGerenciarUsuariosEMaquinas()) {
      throw new NaoAutorizadoException("Somente ADMINISTRADOR pode gerenciar maquinas");
    }
  }
}
