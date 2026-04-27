package com.rgm.api.core.domain.model.aggregates;

import static com.rgm.api.core.domain.validation.DomainValidations.requireNonBlank;
import static com.rgm.api.core.domain.validation.DomainValidations.requireNonNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Anexo de foto/documento. A publicUrl e persistente (sem expiracao) e aponta para o bucket publico
 * (MinIO/S3).
 */
public final class Evidencia {

  private final UUID id;
  private final String publicUrl;
  private final String mimeType;
  private final String nomeArquivo;
  private final Integer tamanhoBytes;
  private final UUID enviadaPorUsuarioId;
  private final Instant criadaEm;

  public Evidencia(
      final UUID id,
      final String publicUrl,
      final String mimeType,
      final String nomeArquivo,
      final Integer tamanhoBytes,
      final UUID enviadaPorUsuarioId,
      final Instant criadaEm) {
    this.id = requireNonNull(id, "id");
    this.publicUrl = requireNonBlank(publicUrl, "publicUrl");
    this.mimeType = requireNonBlank(mimeType, "mimeType");
    this.nomeArquivo = requireNonBlank(nomeArquivo, "nomeArquivo");
    this.tamanhoBytes = tamanhoBytes;
    this.enviadaPorUsuarioId = requireNonNull(enviadaPorUsuarioId, "enviadaPorUsuarioId");
    this.criadaEm = requireNonNull(criadaEm, "criadaEm");

    if (tamanhoBytes != null && tamanhoBytes < 0) {
      throw new IllegalArgumentException("tamanhoBytes deve ser >= 0");
    }
  }

  /** Cria uma nova evidencia com URL persistente. */
  public static Evidencia criar(
      final String publicUrl,
      final String mimeType,
      final String nomeArquivo,
      final Integer tamanhoBytes,
      final UUID enviadaPorUsuarioId,
      final Instant agora) {
    return new Evidencia(
        UUID.randomUUID(),
        publicUrl,
        mimeType,
        nomeArquivo,
        tamanhoBytes,
        enviadaPorUsuarioId,
        agora);
  }

  /** Verifica se o MIME type corresponde a uma imagem (para foto capa). */
  public boolean isImagem() {
    return mimeType != null && mimeType.startsWith("image/");
  }

  public UUID getId() {
    return id;
  }

  public String getPublicUrl() {
    return publicUrl;
  }

  public String getMimeType() {
    return mimeType;
  }

  public String getNomeArquivo() {
    return nomeArquivo;
  }

  public Integer getTamanhoBytes() {
    return tamanhoBytes;
  }

  public UUID getEnviadaPorUsuarioId() {
    return enviadaPorUsuarioId;
  }

  public Instant getCriadaEm() {
    return criadaEm;
  }
}
