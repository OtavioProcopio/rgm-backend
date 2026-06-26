package com.rgm.api.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CriarModeloRequest(
    @NotBlank String codigo,
    @NotBlank String descricao,
    String observacoes,
    @NotBlank String maquina) {}
