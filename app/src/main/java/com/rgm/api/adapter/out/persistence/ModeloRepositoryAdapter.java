package com.rgm.api.adapter.out.persistence;

import com.rgm.api.adapter.out.persistence.mapper.ModeloMapper;
import com.rgm.api.adapter.out.persistence.repository.ModeloJpaRepository;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ModeloRepositoryAdapter implements ModeloRepository {

  private final ModeloJpaRepository jpa;

  public ModeloRepositoryAdapter(final ModeloJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<Modelo> findById(final UUID id) {
    return jpa.findById(id).map(ModeloMapper::toDomain);
  }

  @Override
  public Modelo save(final Modelo modelo) {
    return ModeloMapper.toDomain(jpa.save(ModeloMapper.toJpa(modelo)));
  }

  @Override
  public void deleteById(final UUID id) {
    jpa.deleteById(id);
  }

  @Override
  public int countByMaquinaIdAndCodigo(final UUID maquinaId, final String codigo) {
    return jpa.countByMaquinaIdAndCodigo(maquinaId, codigo);
  }
}
