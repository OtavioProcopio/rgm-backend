package com.rgm.api.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CriarModeloRequest(
    @NotBlank String codigo,
    @NotBlank String descricao,
    String observacoes,
    @NotNull UUID maquinaId) {}
