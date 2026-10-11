package com.rgm.api.adapter.out.persistence;

/** Preparo do termo de busca para consultas com LIKE. */
final class TextoDeBusca {

  private TextoDeBusca() {}

  /** Faz %, _ e a barra invertida valerem como texto no LIKE. */
  static String escaparCuringas(final String termo) {
    return termo.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
