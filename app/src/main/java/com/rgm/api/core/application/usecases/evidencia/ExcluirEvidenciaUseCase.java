package com.rgm.api.core.application.usecases.evidencia;

import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Evidencia;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.ports.repositories.EvidenciaRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoEvidenciaRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.util.UUID;

public final class ExcluirEvidenciaUseCase {

  private final SolicitacaoRepository solicitacaoRepository;
  private final EvidenciaRepository evidenciaRepository;
  private final SolicitacaoEvidenciaRepository solicitacaoEvidenciaRepository;
  private final UsuarioRepository usuarioRepository;
  private final SolicitacaoAtribuicaoRepository atribuicaoRepository;

  public ExcluirEvidenciaUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final EvidenciaRepository evidenciaRepository,
      final SolicitacaoEvidenciaRepository solicitacaoEvidenciaRepository,
      final UsuarioRepository usuarioRepository,
      final SolicitacaoAtribuicaoRepository atribuicaoRepository) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.evidenciaRepository = evidenciaRepository;
    this.solicitacaoEvidenciaRepository = solicitacaoEvidenciaRepository;
    this.usuarioRepository = usuarioRepository;
    this.atribuicaoRepository = atribuicaoRepository;
  }

  public record Input(UUID solicitacaoId, UUID evidenciaId, UUID usuarioId) {}

  public void execute(final Input input) {
    final Solicitacao solicitacao =
        solicitacaoRepository
            .findById(input.solicitacaoId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao nao encontrada"));

    if (solicitacao.getStatus().isTerminal()) {
      throw new BusinessRuleException("Nao e possivel excluir evidencia de solicitacao encerrada");
    }

    final Evidencia evidencia =
        evidenciaRepository
            .findById(input.evidenciaId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Evidencia nao encontrada"));

    if (!solicitacaoEvidenciaRepository.existsBySolicitacaoIdAndEvidenciaId(
        input.solicitacaoId(), input.evidenciaId())) {
      throw new RecursoNaoEncontradoException("Evidencia nao pertence a esta solicitacao");
    }

    validarAcesso(input.solicitacaoId(), input.usuarioId(), evidencia);

    solicitacaoEvidenciaRepository.deleteByEvidenciaId(input.evidenciaId());
    evidenciaRepository.deleteById(input.evidenciaId());
  }

  private void validarAcesso(
      final UUID solicitacaoId, final UUID usuarioId, final Evidencia evidencia) {
    final Usuario usuario =
        usuarioRepository
            .findById(usuarioId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));

    if (usuario.getPerfil().podeGerenciarModelos()
        || usuario.getPerfil().podeGerenciarUsuariosEMaquinas()) {
      return;
    }

    if (evidencia.getEnviadaPorUsuarioId().equals(usuarioId)) {
      return;
    }

    final boolean atribuido =
        atribuicaoRepository.existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(
            solicitacaoId, usuarioId);
    if (!atribuido) {
      throw new NaoAutorizadoException("Usuario nao tem permissao para excluir esta evidencia");
    }
  }
}
