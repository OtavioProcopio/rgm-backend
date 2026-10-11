package com.rgm.api.adapter.in.web.dto.request;

import com.rgm.api.core.domain.validation.LimitesTexto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EncerrarSolicitacaoRequest(
    @NotNull Boolean concluir,
    @NotBlank @Size(max = LimitesTexto.COMENTARIO, message = LimitesTexto.MENSAGEM)
        String comentario) {}
