package com.rgm.api.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CriarMaquinaRequest(
    @NotBlank String nome, @NotBlank String codigo, String descricao) {}
