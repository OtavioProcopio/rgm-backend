package com.rgm.api.adapter.out.persistence;

import com.rgm.api.adapter.out.persistence.mapper.TipoDeModeloMapper;
import com.rgm.api.adapter.out.persistence.repository.TipoDeModeloJpaRepository;
import com.rgm.api.core.domain.model.aggregates.TipoDeModelo;
import com.rgm.api.core.domain.ports.repositories.TipoDeModeloRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class TipoDeModeloRepositoryAdapter implements TipoDeModeloRepository {

  private final TipoDeModeloJpaRepository jpa;

  public TipoDeModeloRepositoryAdapter(final TipoDeModeloJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public TipoDeModelo save(final TipoDeModelo tipoDeModelo) {
    return TipoDeModeloMapper.toDomain(jpa.save(TipoDeModeloMapper.toJpa(tipoDeModelo)));
  }

  @Override
  public Optional<TipoDeModelo> findById(final UUID id) {
    return jpa.findById(id).map(TipoDeModeloMapper::toDomain);
  }

  @Override
  public List<TipoDeModelo> findAll() {
    return jpa.findAll(Sort.by(Sort.Direction.ASC, "nome")).stream()
        .map(TipoDeModeloMapper::toDomain)
        .toList();
  }

  @Override
  public boolean existsByNome(final String nome) {
    return jpa.existsByNomeIgnoreCase(nome);
  }

  @Override
  public boolean existsByNomeAndAtivoTrue(final String nome) {
    return jpa.existsByNomeIgnoreCaseAndAtivoTrue(nome);
  }
}
