package com.rgm.api.adapter.out.persistence;

import com.rgm.api.adapter.out.persistence.mapper.UsuarioMapper;
import com.rgm.api.adapter.out.persistence.repository.UsuarioJpaRepository;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class UsuarioRepositoryAdapter implements UsuarioRepository {

  private final UsuarioJpaRepository jpa;

  public UsuarioRepositoryAdapter(final UsuarioJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<Usuario> findById(final UUID id) {
    return jpa.findById(id).map(UsuarioMapper::toDomain);
  }

  @Override
  public Optional<Usuario> findByEmail(final String email) {
    return jpa.findByEmail(email).map(UsuarioMapper::toDomain);
  }

  @Override
  public List<Usuario> findAllByIdIn(final List<UUID> ids) {
    return jpa.findAllByIdIn(ids).stream().map(UsuarioMapper::toDomain).toList();
  }

  @Override
  public Usuario save(final Usuario usuario) {
    return UsuarioMapper.toDomain(jpa.save(UsuarioMapper.toJpa(usuario)));
  }

  @Override
  public void deleteById(final UUID id) {
    jpa.deleteById(id);
  }

  @Override
  public boolean existsByEmail(final String email) {
    return jpa.existsByEmail(email);
  }
}
