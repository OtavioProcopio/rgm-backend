package com.rgm.api.adapter.out.report;

import static org.junit.jupiter.api.Assertions.*;

import com.rgm.api.core.domain.model.aggregates.EventoModelo;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoAtividadeSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoEventoModelo;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ModeloPdfServiceTest {

  private final ModeloPdfService service = new ModeloPdfService();

  private Modelo buildModelo(final boolean ativo, final boolean temPendencia) {
    final Instant agora = Instant.now();
    return new Modelo(
        UUID.randomUUID(),
        "MOD-001",
        1,
        "Modelo de teste",
        null,
        null,
        null,
        null,
        null,
        ativo,
        "Injetora A",
        temPendencia,
        agora,
        agora);
  }

  private EventoModelo buildEvento(final UUID modeloId) {
    return new EventoModelo(
        UUID.randomUUID(),
        modeloId,
        TipoEventoModelo.MANUTENCAO,
        "Troca de peça",
        "Substituição do rolamento",
        null,
        false,
        UUID.randomUUID(),
        null,
        Instant.now());
  }

  private Solicitacao buildSolicitacao(final UUID modeloId, final StatusSolicitacao status) {
    final boolean concluida = status == StatusSolicitacao.CONCLUIDA;
    final boolean cancelada = status == StatusSolicitacao.CANCELADA;
    return new Solicitacao(
        UUID.randomUUID(),
        "Manutenção urgente",
        "Troca de vedações",
        TipoSolicitacao.REPARO,
        status,
        PrioridadeSolicitacao.ALTA,
        modeloId,
        UUID.randomUUID(),
        concluida || cancelada ? "Encerrado" : null,
        Instant.now(),
        Instant.now(),
        concluida ? Instant.now() : null,
        cancelada ? Instant.now() : null);
  }

  private AtividadeSolicitacao buildAtividade(
      final UUID solicitacaoId, final TipoAtividadeSolicitacao tipo, final String comentario) {
    return new AtividadeSolicitacao(
        UUID.randomUUID(),
        solicitacaoId,
        tipo,
        tipo == TipoAtividadeSolicitacao.MUDANCA_STATUS ? StatusSolicitacao.A_FAZER : null,
        tipo == TipoAtividadeSolicitacao.MUDANCA_STATUS ? StatusSolicitacao.EM_ANDAMENTO : null,
        comentario,
        UUID.randomUUID(),
        Instant.now());
  }

  @Test
  void gerarLista_retornaByteArrayNaoVazio() {
    final var modelos = List.of(buildModelo(true, false), buildModelo(false, true));
    final byte[] pdf = service.gerarLista(modelos);
    assertNotNull(pdf);
    assertTrue(pdf.length > 100);
  }

  @Test
  void gerarLista_listaVazia() {
    final byte[] pdf = service.gerarLista(List.of());
    assertNotNull(pdf);
    assertTrue(pdf.length > 0);
  }

  @Test
  void gerarFicha_modeloSemSolicitacoes() {
    final Modelo modelo = buildModelo(true, false);
    final byte[] pdf = service.gerarFicha(modelo, List.of(), List.of(), Map.of());
    assertNotNull(pdf);
    assertTrue(pdf.length > 100);
  }

  @Test
  void gerarFicha_comEventosESolicitacoes() {
    final Modelo modelo = buildModelo(true, true);
    final UUID modeloId = modelo.getId();
    final var evento = buildEvento(modeloId);
    final var solicitacao = buildSolicitacao(modeloId, StatusSolicitacao.CONCLUIDA);
    final var atividades =
        List.of(
            buildAtividade(solicitacao.getId(), TipoAtividadeSolicitacao.ABERTURA, null),
            buildAtividade(solicitacao.getId(), TipoAtividadeSolicitacao.MUDANCA_STATUS, null),
            buildAtividade(
                solicitacao.getId(), TipoAtividadeSolicitacao.COMENTARIO, "Trocou o rolamento"),
            buildAtividade(
                solicitacao.getId(), TipoAtividadeSolicitacao.EVIDENCIA_ADICIONADA, null),
            buildAtividade(
                solicitacao.getId(), TipoAtividadeSolicitacao.ATRIBUICAO, "Atribuído ao técnico"));

    final byte[] pdf =
        service.gerarFicha(
            modelo, List.of(evento), List.of(solicitacao), Map.of(solicitacao.getId(), atividades));
    assertNotNull(pdf);
    assertTrue(pdf.length > 100);
  }

  @Test
  void gerarFicha_modeloInativo() {
    final Modelo modelo = buildModelo(false, false);
    final byte[] pdf = service.gerarFicha(modelo, List.of(), List.of(), Map.of());
    assertNotNull(pdf);
    assertTrue(pdf.length > 100);
  }

  @Test
  void gerarFicha_solicitacaoSemAtividades() {
    final Modelo modelo = buildModelo(true, false);
    final var solicitacao = buildSolicitacao(modelo.getId(), StatusSolicitacao.A_FAZER);
    final byte[] pdf = service.gerarFicha(modelo, List.of(), List.of(solicitacao), Map.of());
    assertNotNull(pdf);
    assertTrue(pdf.length > 100);
  }

  @Test
  void gerarFicha_multiplosStatus() {
    final Modelo modelo = buildModelo(true, true);
    final var sol1 = buildSolicitacao(modelo.getId(), StatusSolicitacao.A_FAZER);
    final var sol2 = buildSolicitacao(modelo.getId(), StatusSolicitacao.EM_ANDAMENTO);
    final var sol3 = buildSolicitacao(modelo.getId(), StatusSolicitacao.CANCELADA);
    final var sol4 = buildSolicitacao(modelo.getId(), StatusSolicitacao.EM_VALIDACAO);

    final byte[] pdf =
        service.gerarFicha(modelo, List.of(), List.of(sol1, sol2, sol3, sol4), Map.of());
    assertNotNull(pdf);
    assertTrue(pdf.length > 100);
  }
}
