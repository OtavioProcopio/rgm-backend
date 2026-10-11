package com.rgm.api.core.domain.ports.repositories;

import com.rgm.api.core.domain.model.enums.CriterioOrdenacaoSolicitacao;

/**
 * Ordenacao da listagem de solicitacoes: criterio e sentido. Prazo limite crescente traz a mais
 * vencida primeiro; prioridade crescente traz a mais urgente primeiro; sem prazo ou prioridade vai
 * sempre para o fim.
 */
public record OrdenacaoSolicitacao(CriterioOrdenacaoSolicitacao criterio, boolean ascendente) {

  /** Ordem historica da listagem: criacao crescente. */
  public static final OrdenacaoSolicitacao PADRAO =
      new OrdenacaoSolicitacao(CriterioOrdenacaoSolicitacao.CRIADA_EM, true);
}
