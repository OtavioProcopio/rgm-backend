package com.rgm.api.adapter.in.web.dto.response;

public record LoginResponse(String token, String refreshToken, String nome, String perfil) {}
