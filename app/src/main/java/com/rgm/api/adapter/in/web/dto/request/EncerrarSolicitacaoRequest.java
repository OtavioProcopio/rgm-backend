package com.rgm.api.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EncerrarSolicitacaoRequest(@NotNull Boolean concluir, @NotBlank String comentario) {}
