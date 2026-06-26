package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.UUID;

/** UC-12 (parcial): Registrar comentario em solicitacao (Gestor como procurador do externo). */
public final class RegistrarComentarioUseCase {

  private final SolicitacaoRepository solicitacaoRepository;
  private final AtividadeSolicitacaoRepository atividadeRepository;
  private final UsuarioRepository usuarioRepository;
  private final SolicitacaoAtribuicaoRepository atribuicaoRepository;

  public RegistrarComentarioUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final AtividadeSolicitacaoRepository atividadeRepository,
      final UsuarioRepository usuarioRepository,
      final SolicitacaoAtribuicaoRepository atribuicaoRepository) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.atividadeRepository = atividadeRepository;
    this.usuarioRepository = usuarioRepository;
    this.atribuicaoRepository = atribuicaoRepository;
  }

  public record Input(UUID solicitacaoId, String comentario, UUID autorId) {}

  public AtividadeSolicitacao execute(final Input input) {
    final Instant agora = Instant.now();

    final Solicitacao solicitacao =
        solicitacaoRepository
            .findById(input.solicitacaoId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao nao encontrada"));

    final Usuario usuario =
        usuarioRepository
            .findById(input.autorId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));

    if (!usuario.isAtivo()) {
      throw new NaoAutorizadoException("Usuario inativo");
    }

    if (usuario.getPerfil() == PerfilUsuario.EXTERNO) {
      throw new NaoAutorizadoException("Perfil EXTERNO nao pode comentar diretamente");
    }

    if (solicitacao.getStatus().isTerminal()) {
      if (usuario.getPerfil() != PerfilUsuario.GESTOR
          && usuario.getPerfil() != PerfilUsuario.ADMINISTRADOR) {
        throw new NaoAutorizadoException("Nao e possivel comentar em solicitacao encerrada");
      }
    }

    if (usuario.getPerfil() != PerfilUsuario.GESTOR
        && usuario.getPerfil() != PerfilUsuario.ADMINISTRADOR) {
      final boolean ehAutor = solicitacao.getAbertaPorUsuarioId().equals(input.autorId());
      final boolean atribuido =
          atribuicaoRepository.existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(
              input.solicitacaoId(), input.autorId());
      if (!ehAutor && !atribuido) {
        throw new NaoAutorizadoException(
            "Usuario nao tem permissao para comentar nesta solicitacao");
      }
    }

    if (input.comentario() == null || input.comentario().isBlank()) {
      throw new ValidationException("Comentario e obrigatorio");
    }

    final AtividadeSolicitacao atividade =
        AtividadeSolicitacao.comentario(
            input.solicitacaoId(), input.comentario(), input.autorId(), agora);

    return atividadeRepository.save(atividade);
  }
}
