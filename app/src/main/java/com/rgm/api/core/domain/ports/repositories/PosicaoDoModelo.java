package com.rgm.api.core.domain.ports.repositories;

/**
 * Posicao de um modelo na lista do filtro, em ordem de codigo (comeca em 1); {@code posicao} nula
 * quando o modelo existe mas nao atende ao filtro.
 */
public record PosicaoDoModelo(Integer posicao, long total) {}
