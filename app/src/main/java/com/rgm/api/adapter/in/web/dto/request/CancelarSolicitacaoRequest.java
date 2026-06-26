package com.rgm.api.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CancelarSolicitacaoRequest(@NotBlank String motivo) {}
