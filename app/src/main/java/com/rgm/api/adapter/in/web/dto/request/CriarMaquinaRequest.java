package com.rgm.api.adapter.in.web.dto.request;

import com.rgm.api.core.domain.validation.LimitesTexto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarMaquinaRequest(
    @NotBlank @Size(max = LimitesTexto.MAQUINA_NOME, message = LimitesTexto.MENSAGEM)
        String nome) {}
