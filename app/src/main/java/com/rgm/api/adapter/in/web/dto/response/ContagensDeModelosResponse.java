package com.rgm.api.adapter.in.web.dto.response;

import com.rgm.api.core.domain.ports.repositories.ContagensDeModelos;
import java.util.List;

/** Quantos modelos cada botao de tipo e de maquina do catalogo traria. */
public record ContagensDeModelosResponse(
    long total, List<PorTipo> porTipo, List<PorMaquina> porMaquina) {

  public record PorTipo(String tipo, long total) {}

  public record PorMaquina(String maquina, long total) {}

  public static ContagensDeModelosResponse from(final ContagensDeModelos contagens) {
    return new ContagensDeModelosResponse(
        contagens.total(),
        contagens.porTipo().stream().map(q -> new PorTipo(q.valor(), q.total())).toList(),
        contagens.porMaquina().stream().map(q -> new PorMaquina(q.valor(), q.total())).toList());
  }
}
