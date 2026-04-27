package com.rgm.api.core.application.usecases.evidencia;

import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Evidencia;
import com.rgm.api.core.domain.model.entities.SolicitacaoEvidencia;
import com.rgm.api.core.domain.ports.repositories.EvidenciaRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoEvidenciaRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** UC-09: Visualizar evidencias (leitura das publicUrls persistentes). */
public final class VisualizarEvidenciaUseCase {

  private final SolicitacaoRepository solicitacaoRepository;
  private final SolicitacaoEvidenciaRepository solicitacaoEvidenciaRepository;
  private final EvidenciaRepository evidenciaRepository;

  public VisualizarEvidenciaUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final SolicitacaoEvidenciaRepository solicitacaoEvidenciaRepository,
      final EvidenciaRepository evidenciaRepository) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.solicitacaoEvidenciaRepository = solicitacaoEvidenciaRepository;
    this.evidenciaRepository = evidenciaRepository;
  }

  public record Input(UUID solicitacaoId) {}

  public List<Evidencia> execute(final Input input) {
    solicitacaoRepository
        .findById(input.solicitacaoId())
        .orElseThrow(() -> new ValidationException("Solicitacao nao encontrada"));

    final List<SolicitacaoEvidencia> vinculos =
        solicitacaoEvidenciaRepository.findBySolicitacaoId(input.solicitacaoId());

    final List<Evidencia> evidencias = new ArrayList<>();
    for (final SolicitacaoEvidencia vinculo : vinculos) {
      evidenciaRepository.findById(vinculo.getEvidenciaId()).ifPresent(evidencias::add);
    }

    return evidencias;
  }
}
