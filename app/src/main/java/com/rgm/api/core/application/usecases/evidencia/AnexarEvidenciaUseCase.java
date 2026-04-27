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
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** UC-08: Anexar evidencia (upload ao MinIO/S3 com publicUrl persistente). */
public final class AnexarEvidenciaUseCase {
  private static final Logger log = LoggerFactory.getLogger(AnexarEvidenciaUseCase.class);

  private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB
  private static final Set<String> ALLOWED_MIME_TYPES =
      Set.of("image/jpeg", "image/png", "image/gif", "image/webp", "application/pdf", "video/mp4");

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
    log.info("AnexarEvidenciaUseCase.execute iniciado");
    final Instant agora = Instant.now();

    if (input.tamanhoBytes() > MAX_FILE_SIZE_BYTES) {
      throw new ValidationException(
          "Arquivo excede tamanho maximo permitido de "
              + (MAX_FILE_SIZE_BYTES / (1024 * 1024))
              + " MB");
    }

    if (input.mimeType() != null && !ALLOWED_MIME_TYPES.contains(input.mimeType().toLowerCase())) {
      throw new ValidationException(
          "Tipo de arquivo nao permitido: "
              + input.mimeType()
              + ". Tipos aceitos: "
              + ALLOWED_MIME_TYPES);
    }

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
            input.tamanhoBytes() > 0 && input.tamanhoBytes() <= Integer.MAX_VALUE
                ? (int) input.tamanhoBytes()
                : null,
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
