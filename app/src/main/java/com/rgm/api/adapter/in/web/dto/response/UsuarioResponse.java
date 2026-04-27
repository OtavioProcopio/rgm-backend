package com.rgm.api.adapter.in.web.dto.response;

import com.rgm.api.core.domain.model.aggregates.Usuario;
import java.time.Instant;
import java.util.UUID;

public record UsuarioResponse(
    UUID id,
    String nome,
    String email,
    String perfil,
    boolean ativo,
    Instant criadoEm,
    Instant atualizadoEm) {

  public static UsuarioResponse from(final Usuario u) {
    return new UsuarioResponse(
        u.getId(),
        u.getNome(),
        u.getEmail(),
        u.getPerfil().name(),
        u.isAtivo(),
        u.getCriadoEm(),
        u.getAtualizadoEm());
  }
}
