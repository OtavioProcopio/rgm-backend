package com.rgm.api.core.domain.model.enums;

/** Criterio de ordenacao da listagem de solicitacoes. */
public enum CriterioOrdenacaoSolicitacao {
  CRIADA_EM,
  PRAZO_LIMITE,
  PRIORIDADE,
  /** Data de encerramento: conclusao das concluidas e cancelamento das canceladas. */
  ENCERRADA_EM
}
