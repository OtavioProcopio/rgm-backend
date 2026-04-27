package com.rgm.api.core.domain.model.aggregates;

import static com.rgm.api.core.domain.validation.DomainValidations.optionalTrimToNull;
import static com.rgm.api.core.domain.validation.DomainValidations.requireNonBlank;
import static com.rgm.api.core.domain.validation.DomainValidations.requireNonNull;

import java.time.Instant;
import java.util.UUID;

/** Entidade de Maquina (equipamento industrial). */
public final class Maquina {

  private final UUID id;
  private final String nome;
  private final String codigo;
  private final String descricao;
  private final boolean ativa;
  private final Instant criadaEm;
  private final Instant atualizadaEm;

  public Maquina(
      final UUID id,
      final String nome,
      final String codigo,
      final String descricao,
      final boolean ativa,
      final Instant criadaEm,
      final Instant atualizadaEm) {
    this.id = requireNonNull(id, "id");
    this.nome = requireNonBlank(nome, "nome");
    this.codigo = requireNonBlank(codigo, "codigo");
    this.descricao = optionalTrimToNull(descricao);
    this.ativa = ativa;
    this.criadaEm = requireNonNull(criadaEm, "criadaEm");
    this.atualizadaEm = requireNonNull(atualizadaEm, "atualizadaEm");
  }

  /** Cria uma nova maquina ativa. */
  public static Maquina criar(
      final String nome, final String codigo, final String descricao, final Instant agora) {
    return new Maquina(UUID.randomUUID(), nome, codigo, descricao, true, agora, agora);
  }

  public Maquina withAtiva(final boolean novaAtiva, final Instant novoAtualizadaEm) {
    return new Maquina(id, nome, codigo, descricao, novaAtiva, criadaEm, novoAtualizadaEm);
  }

  public Maquina editar(
      final String novoNome,
      final String novoCodigo,
      final String novaDescricao,
      final Instant novoAtualizadaEm) {
    return new Maquina(id, novoNome, novoCodigo, novaDescricao, ativa, criadaEm, novoAtualizadaEm);
  }

  public UUID getId() {
    return id;
  }

  public String getNome() {
    return nome;
  }

  public String getCodigo() {
    return codigo;
  }

  public String getDescricao() {
    return descricao;
  }

  public boolean isAtiva() {
    return ativa;
  }

  public Instant getCriadaEm() {
    return criadaEm;
  }

  public Instant getAtualizadaEm() {
    return atualizadaEm;
  }
}
