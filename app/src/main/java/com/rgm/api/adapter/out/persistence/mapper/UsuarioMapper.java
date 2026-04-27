package com.rgm.api.adapter.out.persistence.mapper;

import com.rgm.api.adapter.out.persistence.entity.UsuarioJpaEntity;
import com.rgm.api.core.domain.model.aggregates.Usuario;

public final class UsuarioMapper {

  private UsuarioMapper() {}

  public static UsuarioJpaEntity toJpa(final Usuario u) {
    return new UsuarioJpaEntity(
        u.getId(),
        u.getNome(),
        u.getEmail(),
        u.getSenhaHash(),
        u.getPerfil(),
        u.isAtivo(),
        u.getCriadoEm(),
        u.getAtualizadoEm());
  }

  public static Usuario toDomain(final UsuarioJpaEntity e) {
    return new Usuario(
        e.getId(),
        e.getNome(),
        e.getEmail(),
        e.getSenhaHash(),
        e.getPerfil(),
        e.isAtivo(),
        e.getCriadoEm(),
        e.getAtualizadoEm());
  }
}
