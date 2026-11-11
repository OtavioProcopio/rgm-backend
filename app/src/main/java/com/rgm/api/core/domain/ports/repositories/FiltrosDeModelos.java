package com.rgm.api.core.domain.ports.repositories;

/**
 * Filtros do catalogo de modelos; todos opcionais e somados. {@code q} procura no codigo ou na
 * descricao, sem caixa e sem acento; {@code maquinaExata} troca "contem" por igualdade em {@code
 * maquina}.
 */
public record FiltrosDeModelos(
    Boolean ativo,
    String codigo,
    String maquina,
    boolean maquinaExata,
    String descricao,
    String tipo,
    String q) {

  /** O mesmo filtro sem o tipo: base da contagem por tipo. */
  public FiltrosDeModelos semTipo() {
    return new FiltrosDeModelos(ativo, codigo, maquina, maquinaExata, descricao, null, q);
  }

  /** O mesmo filtro sem a maquina: base da contagem por maquina. */
  public FiltrosDeModelos semMaquina() {
    return new FiltrosDeModelos(ativo, codigo, null, false, descricao, tipo, q);
  }
}
