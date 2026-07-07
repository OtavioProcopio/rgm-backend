package com.rgm.api.core.application.usecases.admin;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Maquina;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.ports.repositories.MaquinaRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** Cadastrar, renomear, ativar e desativar Maquinas do catalogo. Ator: Administrador. */
@Transactional
public class GerenciarMaquinasUseCase {

  private final MaquinaRepository maquinaRepository;
  private final UsuarioRepository usuarioRepository;

  public GerenciarMaquinasUseCase(
      final MaquinaRepository maquinaRepository, final UsuarioRepository usuarioRepository) {
    this.maquinaRepository = maquinaRepository;
    this.usuarioRepository = usuarioRepository;
  }

  public record CriarInput(String nome, UUID adminId) {}

  public record RenomearInput(UUID maquinaId, String nome, UUID adminId) {}

  public record DesativarInput(UUID maquinaId, UUID adminId) {}

  public record AtivarInput(UUID maquinaId, UUID adminId) {}

  public Maquina criar(final CriarInput input) {
    validarPermissao(input.adminId());
    final String nome = normalizar(input.nome());
    if (maquinaRepository.existsByNome(nome)) {
      throw new ValidationException("Ja existe uma maquina com esse nome");
    }
    return maquinaRepository.save(Maquina.criar(nome, Instant.now()));
  }

  public Maquina renomear(final RenomearInput input) {
    validarPermissao(input.adminId());
    final Maquina maquina = buscar(input.maquinaId());
    final String nome = normalizar(input.nome());
    if (!nome.equalsIgnoreCase(maquina.getNome()) && maquinaRepository.existsByNome(nome)) {
      throw new ValidationException("Ja existe uma maquina com esse nome");
    }
    return maquinaRepository.save(maquina.renomear(nome, Instant.now()));
  }

  public Maquina desativar(final DesativarInput input) {
    validarPermissao(input.adminId());
    final Maquina maquina = buscar(input.maquinaId());
    return maquinaRepository.save(maquina.desativar(Instant.now()));
  }

  public Maquina ativar(final AtivarInput input) {
    validarPermissao(input.adminId());
    final Maquina maquina = buscar(input.maquinaId());
    return maquinaRepository.save(maquina.ativar(Instant.now()));
  }

  private String normalizar(final String nome) {
    if (nome == null || nome.trim().isEmpty()) {
      throw new ValidationException("Nome da maquina e obrigatorio");
    }
    return nome.trim();
  }

  private Maquina buscar(final UUID maquinaId) {
    return maquinaRepository
        .findById(maquinaId)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Maquina nao encontrada"));
  }

  private void validarPermissao(final UUID adminId) {
    final Usuario admin =
        usuarioRepository
            .findById(adminId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));
    if (!admin.getPerfil().podeGerenciarUsuariosEMaquinas()) {
      throw new NaoAutorizadoException("Perfil sem permissao para gerenciar maquinas");
    }
  }
}
