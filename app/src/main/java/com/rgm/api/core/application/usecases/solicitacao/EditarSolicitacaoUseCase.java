package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.UUID;

/** Editar titulo e descricao de uma solicitacao. */
public final class EditarSolicitacaoUseCase {

  private final SolicitacaoRepository solicitacaoRepository;
  private final UsuarioRepository usuarioRepository;
  private final SolicitacaoAtribuicaoRepository atribuicaoRepository;

  public EditarSolicitacaoUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final UsuarioRepository usuarioRepository,
      final SolicitacaoAtribuicaoRepository atribuicaoRepository) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.usuarioRepository = usuarioRepository;
    this.atribuicaoRepository = atribuicaoRepository;
  }

  public record Input(UUID solicitacaoId, String titulo, String descricao, UUID usuarioId) {}

  public Solicitacao execute(final Input input) {
    final Usuario usuario =
        usuarioRepository
            .findById(input.usuarioId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));

    final Solicitacao solicitacao =
        solicitacaoRepository
            .findById(input.solicitacaoId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao nao encontrada"));

    validarPermissao(usuario, solicitacao);

    final Solicitacao editada =
        solicitacao.editar(input.titulo(), input.descricao(), Instant.now());
    return solicitacaoRepository.save(editada);
  }

  private void validarPermissao(final Usuario usuario, final Solicitacao solicitacao) {
    if (usuario.getPerfil() == PerfilUsuario.EXTERNO) {
      throw new NaoAutorizadoException("Perfil EXTERNO nao pode editar solicitacoes");
    }

    if (usuario.getPerfil().podeMoverQualquer()) {
      return;
    }

    if (solicitacao.getAbertaPorUsuarioId().equals(usuario.getId())) {
      return;
    }

    final boolean atribuido =
        atribuicaoRepository.existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(
            solicitacao.getId(), usuario.getId());
    if (!atribuido) {
      throw new NaoAutorizadoException(
          "Operador so pode editar solicitacoes que abriu ou esta atribuido");
    }
  }
}
