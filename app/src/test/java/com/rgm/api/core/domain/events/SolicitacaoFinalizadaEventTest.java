package com.rgm.api.core.domain.events;

import static org.junit.jupiter.api.Assertions.*;

import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SolicitacaoFinalizadaEventTest {

  private static final UUID SOLICITACAO_ID = UUID.randomUUID();
  private static final UUID MODELO_ID = UUID.randomUUID();
  private static final Instant AGORA = Instant.now();

  @Test
  void deveCriarEventoComStatusConcluida() {
    final SolicitacaoFinalizadaEvent event =
        new SolicitacaoFinalizadaEvent(
            SOLICITACAO_ID, MODELO_ID, StatusSolicitacao.CONCLUIDA, AGORA);

    assertEquals(SOLICITACAO_ID, event.getSolicitacaoId());
    assertEquals(MODELO_ID, event.getModeloId());
    assertEquals(StatusSolicitacao.CONCLUIDA, event.getStatusFinal());
    assertEquals(AGORA, event.getOcorridoEm());
  }

  @Test
  void deveCriarEventoComStatusCancelada() {
    final SolicitacaoFinalizadaEvent event =
        new SolicitacaoFinalizadaEvent(
            SOLICITACAO_ID, MODELO_ID, StatusSolicitacao.CANCELADA, AGORA);

    assertEquals(StatusSolicitacao.CANCELADA, event.getStatusFinal());
  }

  @Test
  void deveFalharComSolicitacaoIdNulo() {
    final NullPointerException ex =
        assertThrows(
            NullPointerException.class,
            () ->
                new SolicitacaoFinalizadaEvent(
                    null, MODELO_ID, StatusSolicitacao.CONCLUIDA, AGORA));
    assertTrue(ex.getMessage().contains("solicitacaoId"));
  }

  @Test
  void deveFalharComModeloIdNulo() {
    final NullPointerException ex =
        assertThrows(
            NullPointerException.class,
            () ->
                new SolicitacaoFinalizadaEvent(
                    SOLICITACAO_ID, null, StatusSolicitacao.CONCLUIDA, AGORA));
    assertTrue(ex.getMessage().contains("modeloId"));
  }

  @Test
  void deveFalharComStatusNulo() {
    final NullPointerException ex =
        assertThrows(
            NullPointerException.class,
            () -> new SolicitacaoFinalizadaEvent(SOLICITACAO_ID, MODELO_ID, null, AGORA));
    assertTrue(ex.getMessage().contains("statusFinal"));
  }

  @Test
  void deveFalharComOcorridoEmNulo() {
    final NullPointerException ex =
        assertThrows(
            NullPointerException.class,
            () ->
                new SolicitacaoFinalizadaEvent(
                    SOLICITACAO_ID, MODELO_ID, StatusSolicitacao.CONCLUIDA, null));
    assertTrue(ex.getMessage().contains("ocorridoEm"));
  }

  @Test
  void deveFalharComStatusNaoTerminalAFazer() {
    final IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new SolicitacaoFinalizadaEvent(
                    SOLICITACAO_ID, MODELO_ID, StatusSolicitacao.A_FAZER, AGORA));
    assertTrue(ex.getMessage().contains("A_FAZER"));
  }

  @Test
  void deveFalharComStatusNaoTerminalEmAndamento() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new SolicitacaoFinalizadaEvent(
                SOLICITACAO_ID, MODELO_ID, StatusSolicitacao.EM_ANDAMENTO, AGORA));
  }

  @Test
  void deveFalharComStatusNaoTerminalEmValidacao() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new SolicitacaoFinalizadaEvent(
                SOLICITACAO_ID, MODELO_ID, StatusSolicitacao.EM_VALIDACAO, AGORA));
  }
}
