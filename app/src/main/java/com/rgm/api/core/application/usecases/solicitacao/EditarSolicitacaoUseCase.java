package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** Editar titulo, descricao e tipo de uma solicitacao. */
@Transactional
public class EditarSolicitacaoUseCase {

  private final SolicitacaoRepository solicitacaoRepository;
  private final UsuarioRepository usuarioRepository;
  private final AtividadeSolicitacaoRepository atividadeRepository;

  public EditarSolicitacaoUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final UsuarioRepository usuarioRepository,
      final AtividadeSolicitacaoRepository atividadeRepository) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.usuarioRepository = usuarioRepository;
    this.atividadeRepository = atividadeRepository;
  }

  public record Input(
      UUID solicitacaoId, String titulo, String descricao, TipoSolicitacao tipo, UUID usuarioId) {}

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

    final Instant agora = Instant.now();
    final Solicitacao editada =
        solicitacao.editar(input.titulo(), input.descricao(), input.tipo(), agora);
    final Solicitacao salva = solicitacaoRepository.save(editada);

    atividadeRepository.save(
        AtividadeSolicitacao.comentario(
            salva.getId(),
            "Solicitação editada. Título anterior: "
                + solicitacao.getTitulo()
                + ". Descrição anterior: "
                + solicitacao.getDescricao(),
            input.usuarioId(),
            agora));

    return salva;
  }

  private void validarPermissao(final Usuario usuario, final Solicitacao solicitacao) {
    if (usuario.getPerfil() == PerfilUsuario.EXTERNO) {
      throw new NaoAutorizadoException("Perfil EXTERNO nao pode editar solicitacoes");
    }

    if (usuario.getPerfil().podeMoverQualquer()) {
      return;
    }

    if (!solicitacao.getAbertaPorUsuarioId().equals(usuario.getId())) {
      throw new NaoAutorizadoException("Operador so pode editar solicitacoes que ele abriu");
    }
    if (solicitacao.getStatus() != StatusSolicitacao.A_FAZER) {
      throw new NaoAutorizadoException(
          "Operador so pode editar a solicitacao antes da triagem (em A Fazer)");
    }
  }
}
