package com.rgm.api.core.domain.ports.repositories;

import com.rgm.api.core.domain.model.entities.EventoModeloEvidencia;
import java.util.List;
import java.util.UUID;

public interface EventoModeloEvidenciaRepository {

  EventoModeloEvidencia save(EventoModeloEvidencia eventoModeloEvidencia);

  List<EventoModeloEvidencia> findByEventoModeloId(UUID eventoModeloId);

  boolean existsByEvidenciaIdAndEventoModeloModeloId(UUID evidenciaId, UUID modeloId);
}
