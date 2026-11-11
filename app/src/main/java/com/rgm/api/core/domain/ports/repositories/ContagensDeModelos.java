package com.rgm.api.core.domain.ports.repositories;

import java.util.List;

/**
 * Quantos modelos cada tipo e cada maquina traria. {@code porTipo} ignora o filtro de tipo e {@code
 * porMaquina} o de maquina; {@code total} aplica todos. So entram valores com contagem maior que
 * zero, da maior para a menor, e empate em ordem alfabetica.
 */
public record ContagensDeModelos(
    long total, List<Quantidade> porTipo, List<Quantidade> porMaquina) {

  public record Quantidade(String valor, long total) {}
}
