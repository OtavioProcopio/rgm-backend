package com.rgm.api.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AbrirSolicitacaoRequest(
    @NotBlank String titulo,
    @NotBlank String descricao,
    @NotBlank String tipo,
    @NotNull UUID modeloId) {}
