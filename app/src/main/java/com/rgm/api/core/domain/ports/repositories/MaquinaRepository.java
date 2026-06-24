package com.rgm.api.core.domain.ports.repositories;

import com.rgm.api.core.domain.model.aggregates.Maquina;
import java.util.Optional;
import java.util.UUID;

public interface MaquinaRepository {

  Optional<Maquina> findById(UUID id);

  Maquina save(Maquina maquina);

  void deleteById(UUID id);

  PageResult<Maquina> findAll(int page, int size);

  boolean existsByCodigo(String codigo);
}
