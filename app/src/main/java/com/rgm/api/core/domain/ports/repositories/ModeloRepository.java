package com.rgm.api.core.domain.ports.repositories;

import com.rgm.api.core.domain.model.aggregates.Modelo;
import java.util.Optional;
import java.util.UUID;

public interface ModeloRepository {

  Optional<Modelo> findById(UUID id);

  Modelo save(Modelo modelo);

  void deleteById(UUID id);

  int countByMaquinaIdAndCodigo(UUID maquinaId, String codigo);

  PageResult<Modelo> findAll(int page, int size);

  PageResult<Modelo> findByFilters(Boolean ativo, String codigo, int page, int size);
}
