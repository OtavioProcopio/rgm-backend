package com.rgm.api.core.domain.ports.services;

import com.rgm.api.core.domain.model.aggregates.Usuario;

public interface AccessTokenIssuer {

  String issue(Usuario usuario);
}
