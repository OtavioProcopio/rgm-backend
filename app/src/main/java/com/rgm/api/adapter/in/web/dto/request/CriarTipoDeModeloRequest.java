package com.rgm.api.adapter.in.web.dto.request;

import com.rgm.api.core.domain.validation.LimitesTexto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarTipoDeModeloRequest(
    @NotBlank @Size(max = LimitesTexto.TIPO_DE_MODELO_NOME, message = LimitesTexto.MENSAGEM)
        String nome) {}
