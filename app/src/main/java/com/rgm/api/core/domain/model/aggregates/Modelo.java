package com.rgm.api.core.domain.model.aggregates;

import static com.rgm.api.core.domain.validation.DomainValidations.optionalTrimToNull;
import static com.rgm.api.core.domain.validation.DomainValidations.requireNonBlank;
import static com.rgm.api.core.domain.validation.DomainValidations.requireNonNull;
import static com.rgm.api.core.domain.validation.DomainValidations.requirePositiveOrZero;

import java.time.Instant;
import java.util.UUID;

/** Entidade de Modelo (ferramental) associada a uma Maquina. */
public final class Modelo {

  private final UUID id;
  private final String codigo;
  private final int versao;
  private final String descricao;
  private final String observacoes;
  private final String fotoUrl;
  private final Instant fotoAtualizadaEm;
  private final String estadoAtualDescricao;
  private final Instant estadoAtualAtualizadoEm;
  private final boolean ativo;
  private final String maquina;
  private final boolean temPendenciaAberta;
  private final Instant criadoEm;
  private final Instant atualizadoEm;

  public Modelo(
      final UUID id,
      final String codigo,
      final int versao,
      final String descricao,
      final String observacoes,
      final String fotoUrl,
      final Instant fotoAtualizadaEm,
      final String estadoAtualDescricao,
      final Instant estadoAtualAtualizadoEm,
      final boolean ativo,
      final String maquina,
      final boolean temPendenciaAberta,
      final Instant criadoEm,
      final Instant atualizadoEm) {
    this.id = requireNonNull(id, "id");
    this.codigo = requireNonBlank(codigo, "codigo");
    this.versao = requirePositiveOrZero(versao, "versao");
    this.descricao = requireNonBlank(descricao, "descricao");
    this.observacoes = optionalTrimToNull(observacoes);
    this.fotoUrl = optionalTrimToNull(fotoUrl);
    this.fotoAtualizadaEm = fotoAtualizadaEm;
    this.estadoAtualDescricao = optionalTrimToNull(estadoAtualDescricao);
    this.estadoAtualAtualizadoEm = estadoAtualAtualizadoEm;
    this.ativo = ativo;
    this.maquina = requireNonBlank(maquina, "maquina");
    this.temPendenciaAberta = temPendenciaAberta;
    this.criadoEm = requireNonNull(criadoEm, "criadoEm");
    this.atualizadoEm = requireNonNull(atualizadoEm, "atualizadoEm");
  }

  /** Cria um novo Modelo ativo com versao inicial 1. */
  public static Modelo criar(
      final String codigo,
      final String descricao,
      final String observacoes,
      final String maquina,
      final int versao,
      final Instant agora) {
    return new Modelo(
        UUID.randomUUID(),
        codigo,
        versao,
        descricao,
        observacoes,
        null,
        null,
        null,
        null,
        true,
        maquina,
        false,
        agora,
        agora);
  }

  /** Atualiza o campo temPendenciaAberta (cache derivado). */
  public Modelo withTemPendenciaAberta(final boolean novoValor, final Instant novoAtualizadoEm) {
    return new Modelo(
        id,
        codigo,
        versao,
        descricao,
        observacoes,
        fotoUrl,
        fotoAtualizadaEm,
        estadoAtualDescricao,
        estadoAtualAtualizadoEm,
        ativo,
        maquina,
        novoValor,
        criadoEm,
        novoAtualizadoEm);
  }

  /** Atualiza a foto capa do Modelo (URL persistente do bucket). */
  public Modelo withFotoUrl(final String novaFotoUrl, final Instant novaFotoAtualizadaEm) {
    requireNonBlank(novaFotoUrl, "fotoUrl");
    requireNonNull(novaFotoAtualizadaEm, "fotoAtualizadaEm");
    return new Modelo(
        id,
        codigo,
        versao,
        descricao,
        observacoes,
        novaFotoUrl,
        novaFotoAtualizadaEm,
        estadoAtualDescricao,
        estadoAtualAtualizadoEm,
        ativo,
        maquina,
        temPendenciaAberta,
        criadoEm,
        novaFotoAtualizadaEm);
  }

  /** Atualiza o estado atual (descricao textual da condicao do modelo). */
  public Modelo withEstadoAtual(final String novaDescricao, final Instant novoAtualizadoEm) {
    return new Modelo(
        id,
        codigo,
        versao,
        descricao,
        observacoes,
        fotoUrl,
        fotoAtualizadaEm,
        novaDescricao,
        novoAtualizadoEm,
        ativo,
        maquina,
        temPendenciaAberta,
        criadoEm,
        novoAtualizadoEm);
  }

  /** Desativa o modelo. */
  public Modelo desativar(final Instant novoAtualizadoEm) {
    return new Modelo(
        id,
        codigo,
        versao,
        descricao,
        observacoes,
        fotoUrl,
        fotoAtualizadaEm,
        estadoAtualDescricao,
        estadoAtualAtualizadoEm,
        false,
        maquina,
        temPendenciaAberta,
        criadoEm,
        novoAtualizadoEm);
  }

  /** Ativar o modelo. */
  public Modelo ativar(final Instant novoAtualizadoEm) {
    return new Modelo(
        id,
        codigo,
        versao,
        descricao,
        observacoes,
        fotoUrl,
        fotoAtualizadaEm,
        estadoAtualDescricao,
        estadoAtualAtualizadoEm,
        true,
        maquina,
        temPendenciaAberta,
        criadoEm,
        novoAtualizadoEm);
  }

  /** Edita campos do modelo. */
  public Modelo editar(
      final String novoCodigo,
      final String novaDescricao,
      final String novasObservacoes,
      final String novaMaquina,
      final Instant novoAtualizadoEm) {
    return new Modelo(
        id,
        novoCodigo,
        versao,
        novaDescricao,
        novasObservacoes,
        fotoUrl,
        fotoAtualizadaEm,
        estadoAtualDescricao,
        estadoAtualAtualizadoEm,
        ativo,
        novaMaquina,
        temPendenciaAberta,
        criadoEm,
        novoAtualizadoEm);
  }

  public UUID getId() {
    return id;
  }

  public String getCodigo() {
    return codigo;
  }

  public int getVersao() {
    return versao;
  }

  public String getDescricao() {
    return descricao;
  }

  public String getObservacoes() {
    return observacoes;
  }

  public String getFotoUrl() {
    return fotoUrl;
  }

  public Instant getFotoAtualizadaEm() {
    return fotoAtualizadaEm;
  }

  public String getEstadoAtualDescricao() {
    return estadoAtualDescricao;
  }

  public Instant getEstadoAtualAtualizadoEm() {
    return estadoAtualAtualizadoEm;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public String getMaquina() {
    return maquina;
  }

  public boolean isTemPendenciaAberta() {
    return temPendenciaAberta;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }

  public Instant getAtualizadoEm() {
    return atualizadoEm;
  }
}
