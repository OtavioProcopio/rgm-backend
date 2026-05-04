package com.rgm.api.core.application.usecases.admin;

import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;

/** Listar usuarios com paginacao. */
public final class ListarUsuariosUseCase {

  private final UsuarioRepository usuarioRepository;

  public ListarUsuariosUseCase(final UsuarioRepository usuarioRepository) {
    this.usuarioRepository = usuarioRepository;
  }

  public PageResult<Usuario> execute(final int page, final int size) {
    return usuarioRepository.findAll(page, size);
  }
}
