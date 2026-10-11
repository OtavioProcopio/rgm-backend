package com.rgm.api.adapter.out.report;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SolicitacaoPdfServiceTest {

  private final SolicitacaoPdfService service = new SolicitacaoPdfService();

  private Solicitacao solicitacao(final String titulo, final StatusSolicitacao status) {
    final boolean concluida = status == StatusSolicitacao.CONCLUIDA;
    return new Solicitacao(
        UUID.randomUUID(),
        titulo,
        "Troca de vedações",
        TipoSolicitacao.REPARO,
        status,
        PrioridadeSolicitacao.ALTA,
        UUID.randomUUID(),
        null,
        null,
        null,
        UUID.randomUUID(),
        concluida ? "Concluída com sucesso" : null,
        Instant.parse("2025-01-01T08:00:00Z"),
        Instant.now(),
        concluida ? Instant.parse("2025-01-02T08:00:00Z") : null,
        null);
  }

  private ContextoDoRelatorio contexto(
      final List<String> filtros, final long total, final int exibidas) {
    return new ContextoDoRelatorio(filtros, total, exibidas);
  }

  @Test
  void shouldWriteTheAppliedFiltersAndTheTotalInTheHeader() {
    // Arrange
    final var linhas = List.of(solicitacao("Bomba principal", StatusSolicitacao.A_FAZER));
    final var ctx = contexto(List.of("Status: A fazer", "Máquina: FBOX"), 1, 1);

    // Act
    final String texto = TextoDoPdf.de(service.gerar(linhas, "Gestor Teste", Map.of(), ctx));

    // Assert
    assertTrue(texto.contains("Filtros: Status: A fazer · Máquina: FBOX"));
    assertTrue(texto.contains("Total: 1 solicitação(ões)"));
    assertTrue(texto.contains("Solicitado por: Gestor Teste"));
    assertFalse(texto.contains("Exibindo"));
  }

  @Test
  void shouldSayThereAreNoFiltersWhenNoneWasApplied() {
    // Arrange
    final var ctx = ContextoDoRelatorio.semFiltros(1);
    final var linhas = List.of(solicitacao("Bomba", StatusSolicitacao.EM_ANDAMENTO));

    // Act
    final String texto = TextoDoPdf.de(service.gerar(linhas, "Gestor", Map.of(), ctx));

    // Assert
    assertTrue(texto.contains("Sem filtros"));
  }

  @Test
  void shouldWarnWhenThePdfCarriesFewerRowsThanTheScreenWould() {
    // Arrange
    final var linhas = List.of(solicitacao("Bomba", StatusSolicitacao.A_FAZER));
    final var ctx = contexto(List.of(), 5001, 5000);

    // Act
    final String texto = TextoDoPdf.de(service.gerar(linhas, "Gestor", Map.of(), ctx));

    // Assert
    assertTrue(texto.contains("Exibindo 5000 de 5001"));
    assertTrue(texto.contains("refine os filtros"));
  }

  @Test
  void shouldListEachSolicitacaoTitleInTheTable() {
    // Arrange
    final var linhas =
        List.of(
            solicitacao("Bomba principal", StatusSolicitacao.A_FAZER),
            solicitacao("Correia do eixo", StatusSolicitacao.CONCLUIDA));
    final var ctx = ContextoDoRelatorio.semFiltros(2);

    // Act
    final String texto = TextoDoPdf.de(service.gerar(linhas, "Gestor", Map.of(), ctx));

    // Assert
    assertTrue(texto.contains("Bomba principal"));
    assertTrue(texto.contains("Correia do eixo"));
  }

  @Test
  void shouldGenerateAValidPdfWhenThereAreNoRows() {
    // Arrange
    final var ctx = ContextoDoRelatorio.semFiltros(0);

    // Act
    final byte[] pdf = service.gerar(List.of(), "Gestor", Map.of(), ctx);

    // Assert
    assertTrue(TextoDoPdf.paginas(pdf) >= 1);
    assertTrue(TextoDoPdf.de(pdf).contains("Total: 0 solicitação(ões)"));
  }
}
