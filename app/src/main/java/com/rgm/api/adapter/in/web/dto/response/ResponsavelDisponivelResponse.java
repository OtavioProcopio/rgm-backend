package com.rgm.api.adapter.in.web.dto.response;

import com.rgm.api.core.domain.model.aggregates.Usuario;
import java.util.UUID;

/** Usuario que pode ser escolhido como responsavel por uma solicitacao. */
public record ResponsavelDisponivelResponse(UUID id, String nome, String perfil) {

  public static ResponsavelDisponivelResponse from(final Usuario usuario) {
    return new ResponsavelDisponivelResponse(
        usuario.getId(), usuario.getNome(), usuario.getPerfil().name());
  }
}
