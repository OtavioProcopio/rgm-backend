package com.rgm.api.core.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import org.junit.jupiter.api.Test;

class StatusSolicitacaoTest {

  @Test
  void aFazerDeveTransicionarParaEmAndamentoOuCancelada() {
    assertTrue(StatusSolicitacao.A_FAZER.canTransitionTo(StatusSolicitacao.EM_ANDAMENTO));
    assertTrue(StatusSolicitacao.A_FAZER.canTransitionTo(StatusSolicitacao.CANCELADA));
    assertFalse(StatusSolicitacao.A_FAZER.canTransitionTo(StatusSolicitacao.EM_VALIDACAO));
    assertFalse(StatusSolicitacao.A_FAZER.canTransitionTo(StatusSolicitacao.CONCLUIDA));
    assertFalse(StatusSolicitacao.A_FAZER.canTransitionTo(StatusSolicitacao.A_FAZER));
  }

  @Test
  void emAndamentoDeveTransicionarParaEmValidacaoOuCancelada() {
    assertTrue(StatusSolicitacao.EM_ANDAMENTO.canTransitionTo(StatusSolicitacao.EM_VALIDACAO));
    assertTrue(StatusSolicitacao.EM_ANDAMENTO.canTransitionTo(StatusSolicitacao.CANCELADA));
    assertFalse(StatusSolicitacao.EM_ANDAMENTO.canTransitionTo(StatusSolicitacao.A_FAZER));
    assertFalse(StatusSolicitacao.EM_ANDAMENTO.canTransitionTo(StatusSolicitacao.CONCLUIDA));
  }

  @Test
  void emValidacaoDeveTransicionarParaEmAndamentoConcuidaOuCancelada() {
    assertTrue(StatusSolicitacao.EM_VALIDACAO.canTransitionTo(StatusSolicitacao.EM_ANDAMENTO));
    assertTrue(StatusSolicitacao.EM_VALIDACAO.canTransitionTo(StatusSolicitacao.CONCLUIDA));
    assertTrue(StatusSolicitacao.EM_VALIDACAO.canTransitionTo(StatusSolicitacao.CANCELADA));
    assertFalse(StatusSolicitacao.EM_VALIDACAO.canTransitionTo(StatusSolicitacao.A_FAZER));
  }

  @Test
  void terminaisNaoDeveTransicionar() {
    for (final StatusSolicitacao to : StatusSolicitacao.values()) {
      assertFalse(StatusSolicitacao.CONCLUIDA.canTransitionTo(to));
      assertFalse(StatusSolicitacao.CANCELADA.canTransitionTo(to));
    }
  }

  @Test
  void isTerminalDeveRetornarCorretamente() {
    assertTrue(StatusSolicitacao.CONCLUIDA.isTerminal());
    assertTrue(StatusSolicitacao.CANCELADA.isTerminal());
    assertFalse(StatusSolicitacao.A_FAZER.isTerminal());
    assertFalse(StatusSolicitacao.EM_ANDAMENTO.isTerminal());
    assertFalse(StatusSolicitacao.EM_VALIDACAO.isTerminal());
  }

  @Test
  void transicaoParaNullDeveFalhar() {
    assertFalse(StatusSolicitacao.A_FAZER.canTransitionTo(null));
  }

  @Test
  void exigePrioridadeDeveRetornarCorretamente() {
    assertFalse(StatusSolicitacao.A_FAZER.exigePrioridade());
    assertTrue(StatusSolicitacao.EM_ANDAMENTO.exigePrioridade());
    assertTrue(StatusSolicitacao.EM_VALIDACAO.exigePrioridade());
    assertTrue(StatusSolicitacao.CONCLUIDA.exigePrioridade());
    assertFalse(StatusSolicitacao.CANCELADA.exigePrioridade());
  }
}
