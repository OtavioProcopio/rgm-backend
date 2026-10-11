package com.rgm.api.adapter.in.web.dto.request;

import com.rgm.api.core.domain.validation.LimitesTexto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code tipo} e opcional e ignorado quando ausente — o tipo da solicitacao e imutavel apos a
 * abertura (ver {@code Solicitacao.editar}). Mantido apenas para compatibilidade com clientes que
 * ainda reenviam o tipo atual.
 */
public record EditarSolicitacaoRequest(
    @NotBlank @Size(max = LimitesTexto.SOLICITACAO_TITULO, message = LimitesTexto.MENSAGEM)
        String titulo,
    @NotBlank @Size(max = LimitesTexto.SOLICITACAO_DESCRICAO, message = LimitesTexto.MENSAGEM)
        String descricao,
    String tipo) {}
