package com.rgm.api.adapter.in.web.dto.request;

import com.rgm.api.core.domain.validation.LimitesTexto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EditarInstrucaoRequest(
    @NotBlank @Size(max = LimitesTexto.SOLICITACAO_INSTRUCAO, message = LimitesTexto.MENSAGEM)
        String instrucao) {}
