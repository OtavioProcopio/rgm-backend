package com.rgm.api.adapter.out.persistence;

import com.rgm.api.adapter.out.persistence.mapper.ModeloMapper;
import com.rgm.api.adapter.out.persistence.repository.ModeloJpaRepository;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

  @Override
  public PageResult<Modelo> findAll(final int page, final int size) {
    final var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "codigo"));
    final var result = jpa.findAll(pageable);
    return new PageResult<>(
        result.getContent().stream().map(ModeloMapper::toDomain).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }
}
