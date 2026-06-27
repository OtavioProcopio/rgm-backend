package com.rgm.api.core.domain.model.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventoModeloEvidenciaTest {

  @Test
  void deveCriarEventoModeloEvidencia() {
    final UUID evModId = UUID.randomUUID();
    final UUID evId = UUID.randomUUID();

    final EventoModeloEvidencia rel = new EventoModeloEvidencia(evModId, evId);

    assertNotNull(rel);
    assertEquals(evModId, rel.getEventoModeloId());
    assertEquals(evId, rel.getEvidenciaId());
  }
}
