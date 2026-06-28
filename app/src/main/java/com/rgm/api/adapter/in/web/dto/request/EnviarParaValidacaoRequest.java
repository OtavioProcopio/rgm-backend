package com.rgm.api.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnviarParaValidacaoRequest(
    @NotBlank(message = "Comentário é obrigatório ao enviar para validação")
        @Size(min = 10, max = 1000, message = "Comentário deve ter entre 10 e 1000 caracteres")
        String comentario) {}
