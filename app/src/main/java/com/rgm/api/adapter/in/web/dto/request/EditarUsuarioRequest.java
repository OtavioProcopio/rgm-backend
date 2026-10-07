package com.rgm.api.adapter.in.web.dto.request;

import com.rgm.api.core.domain.validation.LimitesTexto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EditarUsuarioRequest(
    @NotBlank @Size(max = LimitesTexto.USUARIO_NOME, message = LimitesTexto.MENSAGEM) String nome,
    @NotBlank @Email @Size(max = LimitesTexto.USUARIO_EMAIL, message = LimitesTexto.MENSAGEM)
        String email) {}
