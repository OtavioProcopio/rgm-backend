package com.rgm.api.core.application.usecases.evidencia;

import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Evidencia;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.model.entities.SolicitacaoEvidencia;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.EvidenciaRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoEvidenciaRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.services.StorageService;
import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

/** UC-08: Anexar evidencia (upload ao MinIO/S3 com publicUrl persistente). */
public final class AnexarEvidenciaUseCase {

  private final SolicitacaoRepository solicitacaoRepository;
  private final EvidenciaRepository evidenciaRepository;
  private final SolicitacaoEvidenciaRepository solicitacaoEvidenciaRepository;
  private final AtividadeSolicitacaoRepository atividadeRepository;
  private final StorageService storageService;

  public AnexarEvidenciaUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final EvidenciaRepository evidenciaRepository,
      final SolicitacaoEvidenciaRepository solicitacaoEvidenciaRepository,
      final AtividadeSolicitacaoRepository atividadeRepository,
      final StorageService storageService) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.evidenciaRepository = evidenciaRepository;
    this.solicitacaoEvidenciaRepository = solicitacaoEvidenciaRepository;
    this.atividadeRepository = atividadeRepository;
    this.storageService = storageService;
  }

  public record Input(
      UUID solicitacaoId,
      String nomeArquivo,
      String mimeType,
      long tamanhoBytes,
      InputStream conteudo,
      UUID enviadaPorUsuarioId) {}

  public Evidencia execute(final Input input) {
    final Instant agora = Instant.now();

    final Solicitacao solicitacao =
        solicitacaoRepository
            .findById(input.solicitacaoId())
            .orElseThrow(() -> new ValidationException("Solicitacao nao encontrada"));

    if (solicitacao.getStatus().isTerminal()) {
      throw new ValidationException("Nao e possivel anexar evidencia a solicitacao encerrada");
    }

    final String publicUrl =
        storageService.upload(
            input.nomeArquivo(), input.mimeType(), input.conteudo(), input.tamanhoBytes());

    final Evidencia evidencia =
        Evidencia.criar(
            publicUrl,
            input.mimeType(),
            input.nomeArquivo(),
            input.tamanhoBytes() > 0 ? (int) input.tamanhoBytes() : null,
            input.enviadaPorUsuarioId(),
            agora);

    final Evidencia salva = evidenciaRepository.save(evidencia);

    solicitacaoEvidenciaRepository.save(
        new SolicitacaoEvidencia(input.solicitacaoId(), salva.getId()));

    atividadeRepository.save(
        AtividadeSolicitacao.evidenciaAdicionada(
            input.solicitacaoId(), input.enviadaPorUsuarioId(), agora));

    return salva;
  }
}
