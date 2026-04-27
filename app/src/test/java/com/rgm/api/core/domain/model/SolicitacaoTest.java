package com.rgm.api.core.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import com.rgm.api.core.domain.exceptions.TransicaoStatusInvalidaException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SolicitacaoTest {

  private final Instant agora = Instant.now();
  private final UUID modeloId = UUID.randomUUID();
  private final UUID usuarioId = UUID.randomUUID();

  @Test
  void deveAbrirComStatusAFazer() {
    final Solicitacao sol =
        Solicitacao.abrir(
            "Titulo", "Descricao", TipoSolicitacao.REPARO, modeloId, usuarioId, agora);

    assertEquals(StatusSolicitacao.A_FAZER, sol.getStatus());
    assertNull(sol.getPrioridade());
    assertNotNull(sol.getId());
  }

  @Test
  void deveTriarComSucesso() {
    final Solicitacao sol =
        Solicitacao.abrir(
            "Titulo", "Descricao", TipoSolicitacao.REPARO, modeloId, usuarioId, agora);

    final Solicitacao triada = sol.triar(PrioridadeSolicitacao.ALTA, agora);

    assertEquals(StatusSolicitacao.EM_ANDAMENTO, triada.getStatus());
    assertEquals(PrioridadeSolicitacao.ALTA, triada.getPrioridade());
  }

  @Test
  void deveFalharTransicaoInvalida() {
    final Solicitacao sol =
        Solicitacao.abrir(
            "Titulo", "Descricao", TipoSolicitacao.REPARO, modeloId, usuarioId, agora);

    assertThrows(TransicaoStatusInvalidaException.class, () -> sol.enviarParaValidacao(agora));
  }

  @Test
  void deveConcluir() {
    final Solicitacao sol =
        Solicitacao.abrir("T", "D", TipoSolicitacao.REPARO, modeloId, usuarioId, agora);
    final Solicitacao triada = sol.triar(PrioridadeSolicitacao.MEDIA, agora);
    final Solicitacao emValidacao = triada.enviarParaValidacao(agora);
    final Solicitacao concluida = emValidacao.concluir("Feito", agora);

    assertEquals(StatusSolicitacao.CONCLUIDA, concluida.getStatus());
    assertNotNull(concluida.getConcluidaEm());
    assertEquals("Feito", concluida.getComentarioFinal());
  }

  @Test
  void deveCancelar() {
    final Solicitacao sol =
        Solicitacao.abrir("T", "D", TipoSolicitacao.REPARO, modeloId, usuarioId, agora);
    final Solicitacao triada = sol.triar(PrioridadeSolicitacao.MEDIA, agora);
    final Solicitacao emValidacao = triada.enviarParaValidacao(agora);
    final Solicitacao cancelada = emValidacao.cancelar("Motivo", agora);

    assertEquals(StatusSolicitacao.CANCELADA, cancelada.getStatus());
    assertNotNull(cancelada.getCanceladaEm());
  }

  @Test
  void deveFalharSemTitulo() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            Solicitacao.abrir("", "Descricao", TipoSolicitacao.REPARO, modeloId, usuarioId, agora));
  }

  @Test
  void deveDevolver() {
    final Solicitacao sol =
        Solicitacao.abrir("T", "D", TipoSolicitacao.REPARO, modeloId, usuarioId, agora);
    final Solicitacao triada = sol.triar(PrioridadeSolicitacao.MEDIA, agora);
    final Solicitacao emValidacao = triada.enviarParaValidacao(agora);
    final Solicitacao devolvida = emValidacao.devolver(PrioridadeSolicitacao.ALTA, agora);

    assertEquals(StatusSolicitacao.EM_ANDAMENTO, devolvida.getStatus());
    assertEquals(PrioridadeSolicitacao.ALTA, devolvida.getPrioridade());
  }
}
