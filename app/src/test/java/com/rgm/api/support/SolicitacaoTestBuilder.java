package com.rgm.api.support;

import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import java.time.Instant;
import java.util.UUID;

/**
 * Dados de teste de {@link Solicitacao} (Test Data Builder): o teste diz so o que importa e o resto
 * sai valido para o status e o tipo escolhidos, respeitando as invariantes do agregado.
 */
public final class SolicitacaoTestBuilder {

  private static final Instant BASE = Instant.parse("2026-01-01T10:00:00Z");

  private UUID id = UUID.randomUUID();
  private String titulo = "Titulo";
  private String descricao = "Descricao";
  private TipoSolicitacao tipo = TipoSolicitacao.REPARO;
  private StatusSolicitacao status = StatusSolicitacao.A_FAZER;
  private PrioridadeSolicitacao prioridade;
  private UUID abertaPor = UUID.randomUUID();
  private String instrucao;
  private Long version;

  private SolicitacaoTestBuilder() {}

  public static SolicitacaoTestBuilder umaSolicitacao() {
    return new SolicitacaoTestBuilder();
  }

  public SolicitacaoTestBuilder aFazer() {
    return noStatus(StatusSolicitacao.A_FAZER);
  }

  public SolicitacaoTestBuilder emAndamento() {
    return noStatus(StatusSolicitacao.EM_ANDAMENTO);
  }

  public SolicitacaoTestBuilder emValidacao() {
    return noStatus(StatusSolicitacao.EM_VALIDACAO);
  }

  public SolicitacaoTestBuilder concluida() {
    return noStatus(StatusSolicitacao.CONCLUIDA);
  }

  public SolicitacaoTestBuilder cancelada() {
    return noStatus(StatusSolicitacao.CANCELADA);
  }

  public SolicitacaoTestBuilder noStatus(final StatusSolicitacao novo) {
    this.status = novo;
    return this;
  }

  public SolicitacaoTestBuilder doTipoCriacao() {
    this.tipo = TipoSolicitacao.CRIACAO;
    return this;
  }

  public SolicitacaoTestBuilder comId(final UUID valor) {
    this.id = valor;
    return this;
  }

  public SolicitacaoTestBuilder comTitulo(final String valor) {
    this.titulo = valor;
    return this;
  }

  public SolicitacaoTestBuilder comDescricao(final String valor) {
    this.descricao = valor;
    return this;
  }

  public SolicitacaoTestBuilder comPrioridade(final PrioridadeSolicitacao valor) {
    this.prioridade = valor;
    return this;
  }

  public SolicitacaoTestBuilder comInstrucao(final String valor) {
    this.instrucao = valor;
    return this;
  }

  public SolicitacaoTestBuilder abertaPor(final UUID valor) {
    this.abertaPor = valor;
    return this;
  }

  public SolicitacaoTestBuilder comVersion(final Long valor) {
    this.version = valor;
    return this;
  }

  public Solicitacao build() {
    final boolean concluida = status == StatusSolicitacao.CONCLUIDA;
    final boolean cancelada = status == StatusSolicitacao.CANCELADA;
    final boolean criacao = tipo == TipoSolicitacao.CRIACAO;
    final Solicitacao.Builder b =
        Solicitacao.builder()
            .id(id)
            .titulo(titulo)
            .descricao(descricao)
            .tipo(tipo)
            .status(status)
            .prioridade(
                prioridade != null || status == StatusSolicitacao.A_FAZER
                    ? prioridade
                    : PrioridadeSolicitacao.MEDIA)
            .abertaPorUsuarioId(abertaPor)
            .instrucao(instrucao)
            .criadaEm(BASE)
            .atualizadaEm(BASE)
            .version(version);
    if (criacao) {
      b.modeloCodigo("COD-1").modeloMaquina("FBOX").modeloId(concluida ? UUID.randomUUID() : null);
    } else {
      b.modeloId(UUID.randomUUID());
    }
    if (concluida) {
      b.comentarioFinal("Concluida").concluidaEm(BASE);
    }
    if (cancelada) {
      b.comentarioFinal("Cancelada").canceladaEm(BASE);
    }
    return b.build();
  }
}
