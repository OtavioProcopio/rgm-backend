package com.rgm.api.core.domain.ports.services;

import com.rgm.api.core.domain.model.aggregates.Usuario;

public interface AccessTokenIssuer {

  String issue(Usuario usuario);

  String issueRefreshToken(Usuario usuario);

  /** Le um token de acesso; lanca excecao se for invalido, expirado ou de outro tipo. */
  CredencialToken validateAccessToken(String accessToken);

  /** Le um token de renovacao; lanca excecao se for invalido, expirado ou de outro tipo. */
  CredencialToken validateRefreshToken(String refreshToken);
}
