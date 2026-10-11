package com.rgm.api.adapter.in.web.dto.response;

import java.util.UUID;

public record LoginResponse(
    UUID id, String token, String refreshToken, String nome, String perfil) {}
