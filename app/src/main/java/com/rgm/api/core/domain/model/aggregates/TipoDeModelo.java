package com.rgm.api.core.domain.model.aggregates;

import static com.rgm.api.core.domain.validation.DomainValidations.requireNonBlank;
import static com.rgm.api.core.domain.validation.DomainValidations.requireNonNull;

import java.time.Instant;
import java.util.UUID;

/** Tipo de modelo do catalogo administravel referenciado por Modelos. */
public final class TipoDeModelo {

  private final UUID id;
  private final String nome;
  private final boolean ativo;
  private final Instant criadoEm;
  private final Instant atualizadoEm;

  public TipoDeModelo(
      final UUID id,
      final String nome,
      final boolean ativo,
      final Instant criadoEm,
      final Instant atualizadoEm) {
    this.id = requireNonNull(id, "id");
    this.nome = requireNonBlank(nome, "nome");
    this.ativo = ativo;
    this.criadoEm = requireNonNull(criadoEm, "criadoEm");
    this.atualizadoEm = requireNonNull(atualizadoEm, "atualizadoEm");
  }

  /** Cria uma nova TipoDeModelo ativa. */
  public static TipoDeModelo criar(final String nome, final Instant agora) {
    return new TipoDeModelo(UUID.randomUUID(), nome, true, agora, agora);
  }

  /** Renomeia a tipoDeModelo. */
  public TipoDeModelo renomear(final String novoNome, final Instant novoAtualizadoEm) {
    return new TipoDeModelo(id, novoNome, ativo, criadoEm, novoAtualizadoEm);
  }

  /** Desativa a tipoDeModelo. */
  public TipoDeModelo desativar(final Instant novoAtualizadoEm) {
    return new TipoDeModelo(id, nome, false, criadoEm, novoAtualizadoEm);
  }

  /** Ativa a tipoDeModelo. */
  public TipoDeModelo ativar(final Instant novoAtualizadoEm) {
    return new TipoDeModelo(id, nome, true, criadoEm, novoAtualizadoEm);
  }

  public UUID getId() {
    return id;
  }

  public String getNome() {
    return nome;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }

  public Instant getAtualizadoEm() {
    return atualizadoEm;
  }
}
