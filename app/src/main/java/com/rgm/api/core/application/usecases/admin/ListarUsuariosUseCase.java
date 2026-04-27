package com.rgm.api.core.application.usecases.admin;

import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Listar usuarios com paginacao. */
public final class ListarUsuariosUseCase {
  private static final Logger log = LoggerFactory.getLogger(ListarUsuariosUseCase.class);

  private final UsuarioRepository usuarioRepository;

  public ListarUsuariosUseCase(final UsuarioRepository usuarioRepository) {
    this.usuarioRepository = usuarioRepository;
  }

  public PageResult<Usuario> execute(final int page, final int size) {
    log.info("ListarUsuariosUseCase.execute iniciado");
    return usuarioRepository.findAll(page, size);
  }
}
