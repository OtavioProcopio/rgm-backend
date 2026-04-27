package com.rgm.api.core.domain.ports.repositories;

import com.rgm.api.core.domain.model.aggregates.Usuario;
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
}
