package com.rgm.api.core.application.usecases.admin;

import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;

/** Listar usuarios com paginacao e filtros opcionais. */
public final class ListarUsuariosUseCase {

  private final UsuarioRepository usuarioRepository;

  public ListarUsuariosUseCase(final UsuarioRepository usuarioRepository) {
    this.usuarioRepository = usuarioRepository;
  }

  public record Input(PerfilUsuario perfil, Boolean ativo, int page, int size) {}

  public PageResult<Usuario> execute(final Input input) {
    if (input.perfil() != null || input.ativo() != null) {
      return usuarioRepository.findByFilters(
          input.perfil(), input.ativo(), input.page(), input.size());
    }
    return usuarioRepository.findAll(input.page(), input.size());
  }
}
