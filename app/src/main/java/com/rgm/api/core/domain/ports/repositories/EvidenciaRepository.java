package com.rgm.api.core.domain.ports.repositories;

import com.rgm.api.core.domain.model.aggregates.Evidencia;
import java.util.Optional;
import java.util.UUID;

public interface EvidenciaRepository {

  Optional<Evidencia> findById(UUID id);

  Evidencia save(Evidencia evidencia);
}
