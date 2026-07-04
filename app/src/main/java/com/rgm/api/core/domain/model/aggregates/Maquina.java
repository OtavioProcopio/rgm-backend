package com.rgm.api.core.domain.model.aggregates;

import static com.rgm.api.core.domain.validation.DomainValidations.requireNonBlank;
import static com.rgm.api.core.domain.validation.DomainValidations.requireNonNull;

import java.time.Instant;
import java.util.UUID;

/** Maquina do catalogo administravel referenciada por Modelos. */
public final class Maquina {

  private final UUID id;
  private final String nome;
  private final boolean ativo;
  private final Instant criadoEm;
  private final Instant atualizadoEm;

  public Maquina(
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

  /** Cria uma nova Maquina ativa. */
  public static Maquina criar(final String nome, final Instant agora) {
    return new Maquina(UUID.randomUUID(), nome, true, agora, agora);
  }

  /** Renomeia a maquina. */
  public Maquina renomear(final String novoNome, final Instant novoAtualizadoEm) {
    return new Maquina(id, novoNome, ativo, criadoEm, novoAtualizadoEm);
  }

  /** Desativa a maquina. */
  public Maquina desativar(final Instant novoAtualizadoEm) {
    return new Maquina(id, nome, false, criadoEm, novoAtualizadoEm);
  }

  /** Ativa a maquina. */
  public Maquina ativar(final Instant novoAtualizadoEm) {
    return new Maquina(id, nome, true, criadoEm, novoAtualizadoEm);
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
