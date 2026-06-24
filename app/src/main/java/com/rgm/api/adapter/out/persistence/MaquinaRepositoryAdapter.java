package com.rgm.api.adapter.out.persistence;

import com.rgm.api.adapter.out.persistence.mapper.MaquinaMapper;
import com.rgm.api.adapter.out.persistence.repository.MaquinaJpaRepository;
import com.rgm.api.core.domain.model.aggregates.Maquina;
import com.rgm.api.core.domain.ports.repositories.MaquinaRepository;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class MaquinaRepositoryAdapter implements MaquinaRepository {

  private final MaquinaJpaRepository jpa;

  public MaquinaRepositoryAdapter(final MaquinaJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<Maquina> findById(final UUID id) {
    return jpa.findById(id).map(MaquinaMapper::toDomain);
  }

  @Override
  public Maquina save(final Maquina maquina) {
    return MaquinaMapper.toDomain(jpa.save(MaquinaMapper.toJpa(maquina)));
  }

  @Override
  public void deleteById(final UUID id) {
    jpa.deleteById(id);
  }

  @Override
  public PageResult<Maquina> findAll(final int page, final int size) {
    final var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "nome"));
    final var result = jpa.findAll(pageable);
    return new PageResult<>(
        result.getContent().stream().map(MaquinaMapper::toDomain).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Override
  public boolean existsByCodigo(final String codigo) {
    return jpa.existsByCodigo(codigo);
  }
}
