package com.rgm.api.core.application.usecases.evidencia;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Evidencia;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.entities.SolicitacaoEvidencia;
import com.rgm.api.core.domain.ports.repositories.EvidenciaRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoEvidenciaRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** UC-09: Visualizar evidencias (leitura das publicUrls persistentes). */
public final class VisualizarEvidenciaUseCase {

  private final SolicitacaoRepository solicitacaoRepository;
  private final SolicitacaoEvidenciaRepository solicitacaoEvidenciaRepository;
  private final EvidenciaRepository evidenciaRepository;
  private final UsuarioRepository usuarioRepository;
  private final SolicitacaoAtribuicaoRepository atribuicaoRepository;

  public VisualizarEvidenciaUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final SolicitacaoEvidenciaRepository solicitacaoEvidenciaRepository,
      final EvidenciaRepository evidenciaRepository,
      final UsuarioRepository usuarioRepository,
      final SolicitacaoAtribuicaoRepository atribuicaoRepository) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.solicitacaoEvidenciaRepository = solicitacaoEvidenciaRepository;
    this.evidenciaRepository = evidenciaRepository;
    this.usuarioRepository = usuarioRepository;
    this.atribuicaoRepository = atribuicaoRepository;
  }

  public record Input(UUID solicitacaoId, UUID usuarioId) {}

  public List<Evidencia> execute(final Input input) {
    solicitacaoRepository
        .findById(input.solicitacaoId())
        .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao nao encontrada"));

    validarAcesso(input.solicitacaoId(), input.usuarioId());

    final List<SolicitacaoEvidencia> vinculos =
        solicitacaoEvidenciaRepository.findBySolicitacaoId(input.solicitacaoId());

    final List<Evidencia> evidencias = new ArrayList<>();
    for (final SolicitacaoEvidencia vinculo : vinculos) {
      evidenciaRepository.findById(vinculo.getEvidenciaId()).ifPresent(evidencias::add);
    }

    return evidencias;
  }

  private void validarAcesso(final UUID solicitacaoId, final UUID usuarioId) {
    final Usuario usuario =
        usuarioRepository
            .findById(usuarioId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));

    if (usuario.getPerfil().podeGerenciarModelos()
        || usuario.getPerfil().podeGerenciarUsuariosEMaquinas()) {
      return;
    }

    final boolean atribuido =
        atribuicaoRepository.existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(
            solicitacaoId, usuarioId);
    if (!atribuido) {
      throw new NaoAutorizadoException("Usuario nao tem acesso a esta solicitacao");
    }
  }
}
