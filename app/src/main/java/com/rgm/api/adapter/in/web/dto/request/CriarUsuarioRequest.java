package com.rgm.api.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CriarUsuarioRequest(
    @NotBlank String nome, String email, String senha, @NotNull String perfil, boolean ativo) {}
