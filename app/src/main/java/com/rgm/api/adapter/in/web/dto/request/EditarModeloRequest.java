package com.rgm.api.adapter.in.web.dto.request;

import com.rgm.api.core.domain.validation.LimitesTexto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EditarModeloRequest(
    @NotBlank @Size(max = LimitesTexto.MODELO_CODIGO, message = LimitesTexto.MENSAGEM)
        String codigo,
    @NotBlank @Size(max = LimitesTexto.MODELO_DESCRICAO, message = LimitesTexto.MENSAGEM)
        String descricao,
    @Size(max = LimitesTexto.MODELO_OBSERVACOES, message = LimitesTexto.MENSAGEM)
        String observacoes,
    @NotBlank @Size(max = LimitesTexto.MODELO_MAQUINA, message = LimitesTexto.MENSAGEM)
        String maquina,
    String tipo) {}
