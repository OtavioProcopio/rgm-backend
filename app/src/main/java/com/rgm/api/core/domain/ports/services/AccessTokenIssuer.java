package com.rgm.api.core.domain.ports.services;

import com.rgm.api.core.domain.model.aggregates.Usuario;
import java.util.UUID;

public interface AccessTokenIssuer {

  String issue(Usuario usuario);

  String issueRefreshToken(Usuario usuario);

  UUID validateRefreshToken(String refreshToken);
}
