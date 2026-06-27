package com.rgm.api.core.domain.model.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SolicitacaoAtribuicaoTest {

  @Test
  void deveCriarAtribuicaoComSucesso() {
    final UUID solId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final UUID adminId = UUID.randomUUID();
    final Instant agora = Instant.now();

    final SolicitacaoAtribuicao atribuicao = SolicitacaoAtribuicao.criar(solId, userId, adminId, agora);

    assertNotNull(atribuicao.getId());
    assertEquals(solId, atribuicao.getSolicitacaoId());
    assertEquals(userId, atribuicao.getUsuarioId());
    assertEquals(adminId, atribuicao.getAtribuidoPorUsuarioId());
    assertEquals(agora, atribuicao.getAtribuidoEm());
    assertNull(atribuicao.getRemovidoEm());
    assertTrue(atribuicao.isAtiva());
  }

  @Test
  void deveRemoverAtribuicaoAtiva() {
    final UUID solId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final UUID adminId = UUID.randomUUID();
    final Instant agora = Instant.now();
    final Instant posterior = agora.plusSeconds(60);

    final SolicitacaoAtribuicao atribuicao = SolicitacaoAtribuicao.criar(solId, userId, adminId, agora);
    final SolicitacaoAtribuicao removida = atribuicao.remover(posterior);

    assertFalse(removida.isAtiva());
    assertEquals(posterior, removida.getRemovidoEm());
  }

  @Test
  void deveLancarExcecaoAoRemoverAtribuicaoJaRemovida() {
    final UUID solId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final UUID adminId = UUID.randomUUID();
    final Instant agora = Instant.now();

    final SolicitacaoAtribuicao atribuicao = SolicitacaoAtribuicao.criar(solId, userId, adminId, agora);
    final SolicitacaoAtribuicao removida = atribuicao.remover(agora);

    assertThrows(BusinessRuleException.class, () -> removida.remover(agora));
  }
}
