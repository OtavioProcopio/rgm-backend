package com.rgm.api.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record EditarModeloRequest(
    @NotBlank String codigo, @NotBlank String descricao, String observacoes) {}
