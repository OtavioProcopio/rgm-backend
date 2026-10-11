package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.util.UUID;

/** Lista quem pode ser responsavel por uma solicitacao, com busca por nome e paginacao. */
public final class ListarResponsaveisDisponiveisUseCase {

  private final UsuarioRepository usuarioRepository;

  public ListarResponsaveisDisponiveisUseCase(final UsuarioRepository usuarioRepository) {
    this.usuarioRepository = usuarioRepository;
  }

  public record Input(String busca, int page, int size, UUID usuarioId) {}

  public PageResult<Usuario> execute(final Input input) {
    final Usuario usuario =
        usuarioRepository
            .findById(input.usuarioId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));

    if (!usuario.getPerfil().podeTriar()) {
      throw new NaoAutorizadoException("Perfil sem permissao para listar responsaveis");
    }

    return usuarioRepository.buscarAtribuiveis(input.busca(), input.page(), input.size());
  }
}
