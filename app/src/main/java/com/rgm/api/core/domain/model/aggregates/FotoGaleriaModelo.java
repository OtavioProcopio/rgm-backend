package com.rgm.api.core.domain.model.aggregates;

import static com.rgm.api.core.domain.validation.DomainValidations.requireNonBlank;
import static com.rgm.api.core.domain.validation.DomainValidations.requireNonNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Foto da galeria do Modelo (apresentacao/estado atual do ferramental). Distinta de Evidencia:
 * evidencias pertencem ao historico de uma Solicitacao/EventoModelo e nunca alimentam a galeria
 * automaticamente.
 */
public final class FotoGaleriaModelo {

  private final UUID id;
  private final UUID modeloId;
  private final String publicUrl;
  private final String identificacao;
  private final boolean principal;
  private final UUID enviadaPorUsuarioId;
  private final Instant criadoEm;

  public FotoGaleriaModelo(
      final UUID id,
      final UUID modeloId,
      final String publicUrl,
      final String identificacao,
      final boolean principal,
      final UUID enviadaPorUsuarioId,
      final Instant criadoEm) {
    this.id = requireNonNull(id, "id");
    this.modeloId = requireNonNull(modeloId, "modeloId");
    this.publicUrl = requireNonBlank(publicUrl, "publicUrl");
    this.identificacao = requireNonBlank(identificacao, "identificacao");
    this.principal = principal;
    this.enviadaPorUsuarioId = enviadaPorUsuarioId;
    this.criadoEm = requireNonNull(criadoEm, "criadoEm");
  }

  /** Cria uma nova foto de galeria. */
  public static FotoGaleriaModelo criar(
      final UUID modeloId,
      final String publicUrl,
      final String identificacao,
      final boolean principal,
      final UUID enviadaPorUsuarioId,
      final Instant agora) {
    return new FotoGaleriaModelo(
        UUID.randomUUID(),
        modeloId,
        publicUrl,
        identificacao,
        principal,
        enviadaPorUsuarioId,
        agora);
  }

  /** Renomeia a identificacao (rotulo) da foto. */
  public FotoGaleriaModelo comIdentificacao(final String novaIdentificacao) {
    return new FotoGaleriaModelo(
        id, modeloId, publicUrl, novaIdentificacao, principal, enviadaPorUsuarioId, criadoEm);
  }

  /** Marca ou desmarca esta foto como a principal (capa) do modelo. */
  public FotoGaleriaModelo comPrincipal(final boolean novoPrincipal) {
    return new FotoGaleriaModelo(
        id, modeloId, publicUrl, identificacao, novoPrincipal, enviadaPorUsuarioId, criadoEm);
  }

  public UUID getId() {
    return id;
  }

  public UUID getModeloId() {
    return modeloId;
  }

  public String getPublicUrl() {
    return publicUrl;
  }

  public String getIdentificacao() {
    return identificacao;
  }

  public boolean isPrincipal() {
    return principal;
  }

  public UUID getEnviadaPorUsuarioId() {
    return enviadaPorUsuarioId;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }
}
