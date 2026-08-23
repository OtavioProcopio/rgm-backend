package com.rgm.api.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * {@code tipo} e opcional e ignorado quando ausente — o tipo da solicitacao e imutavel apos a
 * abertura (ver {@code Solicitacao.editar}). Mantido apenas para compatibilidade com clientes que
 * ainda reenviam o tipo atual.
 */
public record EditarSolicitacaoRequest(
    @NotBlank String titulo, @NotBlank String descricao, String tipo) {}
