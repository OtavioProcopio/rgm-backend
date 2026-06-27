package com.rgm.api.adapter.out.report;

import static org.junit.jupiter.api.Assertions.*;

import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SolicitacaoPdfServiceTest {

  private final SolicitacaoPdfService service = new SolicitacaoPdfService();

  private Solicitacao buildSolicitacao(final StatusSolicitacao status, final Instant concluidaEm) {
    final String comentario = concluidaEm != null ? "Concluída com sucesso" : null;
    return new Solicitacao(
        UUID.randomUUID(),
        "Manutenção Bomba",
        "Troca de vedações",
        TipoSolicitacao.REPARO,
        status,
        PrioridadeSolicitacao.ALTA,
        UUID.randomUUID(),
        UUID.randomUUID(),
        comentario,
        Instant.parse("2025-01-01T08:00:00Z"),
        Instant.now(),
        concluidaEm,
        null);
  }

  @Test
  void gerar_retornaByteArrayNaoVazio() {
    final byte[] pdf = service.gerar(List.of(buildSolicitacao(StatusSolicitacao.A_FAZER, null)));
    assertNotNull(pdf);
    assertTrue(pdf.length > 100);
  }

  @Test
  void gerar_pdfComListaVazia() {
    final byte[] pdf = service.gerar(List.of());
    assertNotNull(pdf);
    assertTrue(pdf.length > 0);
  }

  @Test
  void gerar_pdfComSolicitacaoConcluida() {
    final Instant concluida = Instant.parse("2025-01-03T10:00:00Z");
    final byte[] pdf =
        service.gerar(List.of(buildSolicitacao(StatusSolicitacao.CONCLUIDA, concluida)));
    assertNotNull(pdf);
    assertTrue(pdf.length > 100);
  }

  @Test
  void gerar_pdfComMultiplasSolicitacoes() {
    final List<Solicitacao> lista =
        List.of(
            buildSolicitacao(StatusSolicitacao.A_FAZER, null),
            buildSolicitacao(StatusSolicitacao.EM_ANDAMENTO, null),
            buildSolicitacao(StatusSolicitacao.CONCLUIDA, Instant.now()));
    final byte[] pdf = service.gerar(lista);
    assertNotNull(pdf);
    assertTrue(pdf.length > 100);
  }
}
