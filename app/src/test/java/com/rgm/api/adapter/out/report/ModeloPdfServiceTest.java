package com.rgm.api.adapter.out.report;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rgm.api.core.application.usecases.modelo.ObterFichaDoModeloUseCase.Ficha;
import com.rgm.api.core.application.usecases.modelo.ObterFichaDoModeloUseCase.FotoDaFicha;
import com.rgm.api.core.domain.model.aggregates.EventoModelo;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoAtividadeSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoEventoModelo;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.MetricaModeloRow;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ModeloPdfServiceTest {

  private final ModeloPdfService service = new ModeloPdfService();

  private Modelo modelo(final String codigo, final String tipo, final boolean ativo) {
    final Instant agora = Instant.now();
    return new Modelo(
        UUID.randomUUID(),
        codigo,
        1,
        "Modelo de teste",
        "Observação do modelo",
        null,
        null,
        ativo,
        "Injetora A",
        tipo,
        false,
        agora,
        agora);
  }

  private EventoModelo evento(final UUID modeloId) {
    return new EventoModelo(
        UUID.randomUUID(),
        modeloId,
        TipoEventoModelo.MANUTENCAO,
        "Troca de peça",
        "Substituição do rolamento",
        null,
        UUID.randomUUID(),
        null,
        Instant.now());
  }

  private Solicitacao solicitacao(final UUID modeloId, final String titulo) {
    return new Solicitacao(
        UUID.randomUUID(),
        titulo,
        "Troca de vedações",
        TipoSolicitacao.REPARO,
        StatusSolicitacao.EM_ANDAMENTO,
        PrioridadeSolicitacao.ALTA,
        modeloId,
        null,
        null,
        null,
        UUID.randomUUID(),
        null,
        Instant.now(),
        Instant.now(),
        null,
        null);
  }

  private AtividadeSolicitacao comentario(final UUID solicitacaoId, final String texto) {
    return new AtividadeSolicitacao(
        UUID.randomUUID(),
        solicitacaoId,
        TipoAtividadeSolicitacao.COMENTARIO,
        null,
        null,
        texto,
        UUID.randomUUID(),
        Instant.now());
  }

  private static byte[] png() {
    try {
      final var imagem = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
      final var saida = new ByteArrayOutputStream();
      ImageIO.write(imagem, "png", saida);
      return saida.toByteArray();
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private Ficha ficha(
      final Modelo modelo,
      final List<Solicitacao> solicitacoes,
      final Map<UUID, List<AtividadeSolicitacao>> atividades,
      final List<FotoDaFicha> fotos) {
    return new Ficha(
        modelo,
        List.of(evento(modelo.getId())),
        solicitacoes,
        atividades,
        Map.of(),
        7200.0,
        null,
        fotos);
  }

  // ── lista ────────────────────────────────────────────────────────────────

  @Test
  void shouldWriteFiltersTotalAndTypeInTheModelList() {
    // Arrange
    final var modelos = List.of(modelo("MOD-001", "RESINA", true));
    final var ctx = new ContextoDoRelatorio(List.of("Situação: ativos", "Máquina: FBOX"), 1, 1);

    // Act
    final String texto = TextoDoPdf.de(service.gerarLista(modelos, "Gestor", ctx));

    // Assert
    assertTrue(texto.contains("Filtros: Situação: ativos · Máquina: FBOX"));
    assertTrue(texto.contains("1 modelo(s)"));
    assertTrue(texto.contains("Tipo"));
    assertTrue(texto.contains("RESINA"));
    assertTrue(texto.contains("MOD-001"));
  }

  @Test
  void shouldWarnWhenTheModelListWasCut() {
    // Arrange
    final var modelos = List.of(modelo("MOD-001", null, true));
    final var ctx = new ContextoDoRelatorio(List.of(), 6000, 5000);

    // Act
    final String texto = TextoDoPdf.de(service.gerarLista(modelos, "Gestor", ctx));

    // Assert
    assertTrue(texto.contains("Exibindo 5000 de 6000"));
  }

  // ── ranking ──────────────────────────────────────────────────────────────

  @Test
  void shouldWriteOrderingAndRowsInTheRanking() {
    // Arrange
    final var linhas = List.of(new MetricaModeloRow(UUID.randomUUID(), "MOD-9", 3600.0, null));
    final var ctx = new ContextoDoRelatorio(List.of("Ordenado por: Tempo resolucao"), 1, 1);

    // Act
    final String texto = TextoDoPdf.de(service.gerarRankingModelos(linhas, "Gestor", ctx));

    // Assert
    assertTrue(texto.contains("Ordenado por: Tempo resolucao"));
    assertTrue(texto.contains("MOD-9"));
  }

  // ── ficha ────────────────────────────────────────────────────────────────

  @Test
  void shouldIncludeHistoryAndPhotosWithTheCoverHighlightedInTheFicha() {
    // Arrange
    final var modelo = modelo("MOD-500", "COQUILHA_ACO", true);
    final var solicitacao = solicitacao(modelo.getId(), "Trocar vedação");
    final var atividades =
        Map.of(solicitacao.getId(), List.of(comentario(solicitacao.getId(), "Peça substituída")));
    final var fotos =
        List.of(
            new FotoDaFicha("Foto de capa", true, png()),
            new FotoDaFicha("Detalhe lateral", false, png()),
            new FotoDaFicha("Vista superior", false, png()));

    // Act
    final byte[] pdf =
        service.gerarFicha(ficha(modelo, List.of(solicitacao), atividades, fotos), "Gestor");

    // Assert
    final String texto = TextoDoPdf.de(pdf);
    assertTrue(texto.contains("MOD-500"));
    assertTrue(texto.contains("Fotos do Modelo"));
    assertTrue(texto.contains("Capa — Foto de capa"));
    assertTrue(texto.contains("Detalhe lateral"));
    assertTrue(texto.contains("Eventos do Modelo"));
    assertTrue(texto.contains("Troca de peça"));
    assertTrue(texto.contains("Histórico de Solicitações"));
    assertTrue(texto.contains("Trocar vedação"));
    assertTrue(texto.contains("Peça substituída"));
    assertFalse(texto.contains("Foto indisponível"));
  }

  @Test
  void shouldKeepTheFichaAndMarkThePhotoWhenItCouldNotBeRead() {
    // Arrange
    final var modelo = modelo("MOD-501", null, true);
    final var fotos = List.of(new FotoDaFicha("Foto perdida", false, null));

    // Act
    final byte[] pdf = service.gerarFicha(ficha(modelo, List.of(), Map.of(), fotos), "Gestor");

    // Assert
    final String texto = TextoDoPdf.de(pdf);
    assertTrue(texto.contains("MOD-501"));
    assertTrue(texto.contains("Foto perdida"));
    assertTrue(texto.contains("Foto indisponível"));
  }

  @Test
  void shouldOmitThePhotoSectionWhenTheModelHasNoPhotos() {
    // Arrange
    final var modelo = modelo("MOD-502", null, false);

    // Act
    final byte[] pdf = service.gerarFicha(ficha(modelo, List.of(), Map.of(), List.of()), "Gestor");

    // Assert
    assertFalse(TextoDoPdf.de(pdf).contains("Fotos do Modelo"));
  }
}
