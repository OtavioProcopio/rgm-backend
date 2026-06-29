package com.rgm.api.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EditarSolicitacaoRequest(
    @NotBlank String titulo, @NotBlank String descricao, @NotNull String tipo) {}
