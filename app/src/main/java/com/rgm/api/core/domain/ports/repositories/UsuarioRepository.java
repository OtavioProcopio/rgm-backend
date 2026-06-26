package com.rgm.api.core.domain.ports.repositories;

import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository {

  Optional<Usuario> findById(UUID id);

  Optional<Usuario> findByEmail(String email);

  List<Usuario> findAllByIdIn(List<UUID> ids);

  Usuario save(Usuario usuario);

  void deleteById(UUID id);

  boolean existsByEmail(String email);

  boolean existsByEmailAndIdNot(String email, UUID excludeId);

  PageResult<Usuario> findAll(int page, int size);

  PageResult<Usuario> findByFilters(PerfilUsuario perfil, Boolean ativo, int page, int size);

  long count();
}
