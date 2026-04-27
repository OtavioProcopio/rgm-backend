package com.rgm.api.core.domain.ports.repositories;

import com.rgm.api.core.domain.model.aggregates.EventoModelo;
import java.util.List;
import java.util.UUID;

public interface EventoModeloRepository {

  EventoModelo save(EventoModelo eventoModelo);

  List<EventoModelo> findByModeloId(UUID modeloId);
}
