package com.rgm.api.core.domain.model.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class SolicitacaoEvidenciaTest {

  @Test
  void deveCriarSolicitacaoEvidencia() {
    final UUID solId = UUID.randomUUID();
    final UUID evId = UUID.randomUUID();

    final SolicitacaoEvidencia rel = new SolicitacaoEvidencia(solId, evId);

    assertNotNull(rel);
    assertEquals(solId, rel.getSolicitacaoId());
    assertEquals(evId, rel.getEvidenciaId());
  }
}
