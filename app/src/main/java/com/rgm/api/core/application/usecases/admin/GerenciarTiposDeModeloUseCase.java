package com.rgm.api.core.application.usecases.admin;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.TipoDeModelo;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.TipoDeModeloRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** Cadastrar, renomear, ativar e desativar TiposDeModelo do catalogo. Ator: Administrador. */
@Transactional
public class GerenciarTiposDeModeloUseCase {

  private final TipoDeModeloRepository tipoDeModeloRepository;
  private final UsuarioRepository usuarioRepository;
  private final ModeloRepository modeloRepository;

  public GerenciarTiposDeModeloUseCase(
      final TipoDeModeloRepository tipoDeModeloRepository,
      final UsuarioRepository usuarioRepository,
      final ModeloRepository modeloRepository) {
    this.tipoDeModeloRepository = tipoDeModeloRepository;
    this.usuarioRepository = usuarioRepository;
    this.modeloRepository = modeloRepository;
  }

  public record CriarInput(String nome, UUID adminId) {}

  public record RenomearInput(UUID tipoDeModeloId, String nome, UUID adminId) {}

  public record DesativarInput(UUID tipoDeModeloId, UUID adminId) {}

  public record AtivarInput(UUID tipoDeModeloId, UUID adminId) {}

  public TipoDeModelo criar(final CriarInput input) {
    validarPermissao(input.adminId());
    final String nome = normalizar(input.nome());
    if (tipoDeModeloRepository.existsByNome(nome)) {
      throw new ValidationException("Ja existe um tipo de modelo com esse nome");
    }
    return tipoDeModeloRepository.save(TipoDeModelo.criar(nome, Instant.now()));
  }

  public TipoDeModelo renomear(final RenomearInput input) {
    validarPermissao(input.adminId());
    final TipoDeModelo tipoDeModelo = buscar(input.tipoDeModeloId());
    final String nome = normalizar(input.nome());
    if (!nome.equalsIgnoreCase(tipoDeModelo.getNome())
        && tipoDeModeloRepository.existsByNome(nome)) {
      throw new ValidationException("Ja existe um tipo de modelo com esse nome");
    }
    final TipoDeModelo renomeado =
        tipoDeModeloRepository.save(tipoDeModelo.renomear(nome, Instant.now()));
    if (!tipoDeModelo.getNome().equals(nome)) {
      modeloRepository.trocarTipo(tipoDeModelo.getNome(), nome);
    }
    return renomeado;
  }

  public TipoDeModelo desativar(final DesativarInput input) {
    validarPermissao(input.adminId());
    final TipoDeModelo tipoDeModelo = buscar(input.tipoDeModeloId());
    return tipoDeModeloRepository.save(tipoDeModelo.desativar(Instant.now()));
  }

  public TipoDeModelo ativar(final AtivarInput input) {
    validarPermissao(input.adminId());
    final TipoDeModelo tipoDeModelo = buscar(input.tipoDeModeloId());
    return tipoDeModeloRepository.save(tipoDeModelo.ativar(Instant.now()));
  }

  private String normalizar(final String nome) {
    if (nome == null || nome.trim().isEmpty()) {
      throw new ValidationException("Nome do tipo de modelo e obrigatorio");
    }
    return nome.trim();
  }

  private TipoDeModelo buscar(final UUID tipoDeModeloId) {
    return tipoDeModeloRepository
        .findById(tipoDeModeloId)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Tipo de modelo nao encontrado"));
  }

  private void validarPermissao(final UUID adminId) {
    final Usuario admin =
        usuarioRepository
            .findById(adminId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));
    if (!admin.getPerfil().podeGerenciarUsuariosEMaquinas()) {
      throw new NaoAutorizadoException("Perfil sem permissao para gerenciar tipos de modelo");
    }
  }
}
