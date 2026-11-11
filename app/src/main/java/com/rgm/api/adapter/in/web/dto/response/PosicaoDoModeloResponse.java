package com.rgm.api.adapter.in.web.dto.response;

import com.rgm.api.core.domain.ports.repositories.PosicaoDoModelo;

/** Posicao do modelo na lista do filtro (comeca em 1); nula se ele nao atende ao filtro. */
public record PosicaoDoModeloResponse(Integer posicao, long total) {

  public static PosicaoDoModeloResponse from(final PosicaoDoModelo posicao) {
    return new PosicaoDoModeloResponse(posicao.posicao(), posicao.total());
  }
}
