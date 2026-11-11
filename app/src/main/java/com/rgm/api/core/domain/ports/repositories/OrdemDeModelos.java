package com.rgm.api.core.domain.ports.repositories;

/** Ordem da listagem de modelos. */
public enum OrdemDeModelos {
  /** Mais recente primeiro (a ordem de sempre da listagem). */
  CRIACAO,
  /** Codigo em ordem alfabetica; no mesmo codigo, versao maior primeiro; depois o id. */
  CODIGO
}
