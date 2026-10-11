package com.rgm.api.core.domain.ports.repositories;

import com.rgm.api.core.domain.model.aggregates.TipoDeModelo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TipoDeModeloRepository {

  TipoDeModelo save(TipoDeModelo tipoDeModelo);

  Optional<TipoDeModelo> findById(UUID id);

  List<TipoDeModelo> findAll();

  boolean existsByNome(String nome);

  boolean existsByNomeAndAtivoTrue(String nome);
}
