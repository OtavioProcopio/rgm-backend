package com.rgm.api.core.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.TransicaoStatusInvalidaException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
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

  @Test
  void prazoLimiteENuloAntesDaTriagem() {
    final Solicitacao sol =
        Solicitacao.abrir("T", "D", TipoSolicitacao.REPARO, modeloId, usuarioId, agora);

    assertNull(sol.getPrazoLimite());
    assertNull(sol.getTempoRestanteSegundos(agora));
    assertFalse(sol.isAtrasada(agora));
    assertNull(sol.getTempoResolucaoSegundos());
  }

  @Test
  void prazoLimiteEComputadoAPartirDaPrioridadeAposTriagem() {
    final Solicitacao sol =
        Solicitacao.abrir("T", "D", TipoSolicitacao.REPARO, modeloId, usuarioId, agora);
    final Solicitacao triada = sol.triar(PrioridadeSolicitacao.URGENTE, agora);

    assertEquals(agora.plusSeconds(4 * 3600L), triada.getPrazoLimite());
    assertFalse(triada.isAtrasada(agora));
  }

  @Test
  void tempoRestanteFicaNegativoQuandoPrazoVenceu() {
    final Solicitacao sol =
        Solicitacao.abrir("T", "D", TipoSolicitacao.REPARO, modeloId, usuarioId, agora);
    final Solicitacao triada = sol.triar(PrioridadeSolicitacao.URGENTE, agora);
    final Instant depoisDoPrazo = agora.plusSeconds(5 * 3600L);

    assertTrue(triada.getTempoRestanteSegundos(depoisDoPrazo) < 0);
    assertTrue(triada.isAtrasada(depoisDoPrazo));
  }

  @Test
  void tempoRestanteENuloEmStatusTerminal() {
    final Solicitacao sol =
        Solicitacao.abrir("T", "D", TipoSolicitacao.REPARO, modeloId, usuarioId, agora);
    final Solicitacao triada = sol.triar(PrioridadeSolicitacao.URGENTE, agora);
    final Solicitacao emValidacao = triada.enviarParaValidacao(agora);
    final Instant depoisDoPrazo = agora.plusSeconds(5 * 3600L);
    final Solicitacao concluida = emValidacao.concluir("Feito", depoisDoPrazo);

    assertNull(concluida.getTempoRestanteSegundos(depoisDoPrazo));
    assertTrue(concluida.isAtrasada(depoisDoPrazo));
    assertEquals(5 * 3600L, concluida.getTempoResolucaoSegundos());
  }

  @Test
  void canceladaNuncaContaComoAtrasada() {
    final Solicitacao sol =
        Solicitacao.abrir("T", "D", TipoSolicitacao.REPARO, modeloId, usuarioId, agora);
    final Solicitacao triada = sol.triar(PrioridadeSolicitacao.URGENTE, agora);
    final Instant depoisDoPrazo = agora.plusSeconds(5 * 3600L);
    final Solicitacao cancelada = triada.cancelar("Motivo", depoisDoPrazo);

    assertFalse(cancelada.isAtrasada(depoisDoPrazo));
  }

  @Test
  void abrirCriacaoNaoTemModeloVinculado() {
    final Solicitacao sol =
        Solicitacao.abrirCriacao(
            "Novo modelo XYZ", "Descricao pretendida", "COD-XYZ", "FBOX", null, usuarioId, agora);

    assertEquals(TipoSolicitacao.CRIACAO, sol.getTipo());
    assertEquals(StatusSolicitacao.A_FAZER, sol.getStatus());
    assertNull(sol.getModeloId());
    assertEquals("COD-XYZ", sol.getModeloCodigo());
    assertEquals("FBOX", sol.getModeloMaquina());
  }

  @Test
  void abrirNaoCriacaoExigeModeloId() {
    assertThrows(
        ValidationException.class,
        () ->
            new Solicitacao(
                UUID.randomUUID(),
                "T",
                "D",
                TipoSolicitacao.REPARO,
                StatusSolicitacao.A_FAZER,
                null,
                null,
                null,
                null,
                null,
                usuarioId,
                null,
                agora,
                agora,
                null,
                null));
  }

  @Test
  void criacaoExigeCodigoEMaquina() {
    assertThrows(
        ValidationException.class,
        () ->
            new Solicitacao(
                UUID.randomUUID(),
                "T",
                "D",
                TipoSolicitacao.CRIACAO,
                StatusSolicitacao.A_FAZER,
                null,
                null,
                null,
                null,
                null,
                usuarioId,
                null,
                agora,
                agora,
                null,
                null));
  }

  @Test
  void criacaoNaoPodeTerModeloIdAntesDeConcluida() {
    assertThrows(
        ValidationException.class,
        () ->
            new Solicitacao(
                UUID.randomUUID(),
                "T",
                "D",
                TipoSolicitacao.CRIACAO,
                StatusSolicitacao.A_FAZER,
                null,
                modeloId,
                "COD",
                "FBOX",
                null,
                usuarioId,
                null,
                agora,
                agora,
                null,
                null));
  }

  @Test
  void concluirCriacaoVinculaOModeloRecemCriado() {
    final Solicitacao sol =
        Solicitacao.abrirCriacao(
            "Novo modelo XYZ", "Descricao", "COD-XYZ", "FBOX", null, usuarioId, agora);
    final Solicitacao triada = sol.triar(PrioridadeSolicitacao.MEDIA, agora);
    final Solicitacao emValidacao = triada.enviarParaValidacao(agora);
    final UUID modeloRecemCriado = UUID.randomUUID();

    final Solicitacao concluida =
        emValidacao.concluirCriacao("Modelo criado", modeloRecemCriado, agora);

    assertEquals(StatusSolicitacao.CONCLUIDA, concluida.getStatus());
    assertEquals(modeloRecemCriado, concluida.getModeloId());
  }

  @Test
  void concluirNaoSeAplicaAoTipoCriacao() {
    final Solicitacao sol =
        Solicitacao.abrirCriacao(
            "Novo modelo XYZ", "Descricao", "COD-XYZ", "FBOX", null, usuarioId, agora);
    final Solicitacao triada = sol.triar(PrioridadeSolicitacao.MEDIA, agora);
    final Solicitacao emValidacao = triada.enviarParaValidacao(agora);

    assertThrows(BusinessRuleException.class, () -> emValidacao.concluir("Feito", agora));
  }

  @Test
  void concluirCriacaoNaoSeAplicaAOutrosTipos() {
    final Solicitacao sol =
        Solicitacao.abrir("T", "D", TipoSolicitacao.REPARO, modeloId, usuarioId, agora);
    final Solicitacao triada = sol.triar(PrioridadeSolicitacao.MEDIA, agora);
    final Solicitacao emValidacao = triada.enviarParaValidacao(agora);

    assertThrows(
        BusinessRuleException.class,
        () -> emValidacao.concluirCriacao("Feito", UUID.randomUUID(), agora));
  }

  @Test
  void editarRejeitaMudancaDeTipo() {
    final Solicitacao sol =
        Solicitacao.abrir("T", "D", TipoSolicitacao.REPARO, modeloId, usuarioId, agora);

    assertThrows(
        BusinessRuleException.class,
        () -> sol.editar("Novo titulo", "Nova desc", TipoSolicitacao.INSPECAO, agora));
  }

  @Test
  void editarPermiteManterOMesmoTipo() {
    final Solicitacao sol =
        Solicitacao.abrir("T", "D", TipoSolicitacao.REPARO, modeloId, usuarioId, agora);

    final Solicitacao editada =
        sol.editar("Novo titulo", "Nova desc", TipoSolicitacao.REPARO, agora);

    assertEquals("Novo titulo", editada.getTitulo());
    assertEquals(TipoSolicitacao.REPARO, editada.getTipo());
  }

  @Test
  void gestorPodeAbrirCriacao() {
    assertDoesNotThrow(() -> Solicitacao.validarAutorizacaoAbrirCriacao(PerfilUsuario.GESTOR));
    assertDoesNotThrow(
        () -> Solicitacao.validarAutorizacaoAbrirCriacao(PerfilUsuario.ADMINISTRADOR));
  }

  @Test
  void operadorNaoPodeAbrirCriacao() {
    assertThrows(
        NaoAutorizadoException.class,
        () -> Solicitacao.validarAutorizacaoAbrirCriacao(PerfilUsuario.OPERADOR));
  }
}
