package com.rgm.api.core.domain.model.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.rgm.api.core.domain.model.enums.TipoAtividadeSolicitacao;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AtividadeSolicitacaoTest {

  private final UUID solicitacaoId = UUID.randomUUID();
  private final UUID autorId = UUID.randomUUID();
  private final Instant agora = Instant.parse("2026-11-09T12:00:00Z");

  @Test
  void shouldKeepThePreviousInstrucaoAndWhoChangedItWhenInstrucaoAlterada() {
    // Act
    final AtividadeSolicitacao a =
        AtividadeSolicitacao.instrucaoAlterada(solicitacaoId, "  Texto anterior ", autorId, agora);

    // Assert
    assertNotNull(a.getId());
    assertEquals(TipoAtividadeSolicitacao.INSTRUCAO_ALTERADA, a.getTipo());
    assertEquals(solicitacaoId, a.getSolicitacaoId());
    assertEquals("Texto anterior", a.getComentario());
    assertEquals(autorId, a.getAutorUsuarioId());
    assertEquals(agora, a.getCriadaEm());
    assertNull(a.getDeStatus());
    assertNull(a.getParaStatus());
  }

  @Test
  void shouldHaveNoPreviousTextWhenTheSolicitacaoHadNoInstrucao() {
    // Act
    final AtividadeSolicitacao a =
        AtividadeSolicitacao.instrucaoAlterada(solicitacaoId, null, autorId, agora);

    // Assert
    assertNull(a.getComentario());
  }
}
