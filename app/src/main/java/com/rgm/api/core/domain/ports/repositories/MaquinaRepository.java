package com.rgm.api.core.domain.ports.repositories;

import com.rgm.api.core.domain.model.aggregates.Maquina;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MaquinaRepository {

  Maquina save(Maquina maquina);

  Optional<Maquina> findById(UUID id);

  List<Maquina> findAll();

  boolean existsByNome(String nome);

  boolean existsByNomeAndAtivoTrue(String nome);
}
