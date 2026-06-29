package com.rgm.api.adapter.out.report;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SolicitacaoPdfService {

  private static final DateTimeFormatter FMT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Sao_Paulo"));
  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.of("America/Sao_Paulo"));

  private static final Color HEADER_BG = new Color(41, 65, 122);
  private static final Color ROW_ALT = new Color(245, 247, 250);
  private static final Color BORDER_COLOR = new Color(220, 220, 220);

  private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15);
  private static final Font SUBTITLE_FONT =
      FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(80, 80, 80));
  private static final Font AUDIT_FONT =
      FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, new Color(100, 100, 100));
  private static final Font HEADER_FONT =
      FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
  private static final Font CELL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 7);
  private static final Font CELL_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7);

  /**
   * Gera o relatório de solicitações com auditoria completa.
   *
   * @param solicitacoes lista filtrada
   * @param geradoPorNome nome do usuário que solicitou o PDF
   * @param nomesPorUsuario mapa UUID → nome para resolução de autores
   */
  public byte[] gerar(
      final List<Solicitacao> solicitacoes,
      final String geradoPorNome,
      final Map<UUID, String> nomesPorUsuario) {

    final var out = new ByteArrayOutputStream();
    final var doc = new Document(PageSize.A4.rotate(), 40, 40, 55, 45);
    final PdfWriter writer = PdfWriter.getInstance(doc, out);
    writer.setPageEvent(new PdfFooterEvent(geradoPorNome));
    doc.open();

    addCabecalho(doc, solicitacoes.size(), geradoPorNome);
    addTabela(doc, solicitacoes, nomesPorUsuario);

    doc.close();
    return out.toByteArray();
  }

  /** Mantém compatibilidade com chamadas sem resolução de usuários. */
  public byte[] gerar(final List<Solicitacao> solicitacoes) {
    return gerar(solicitacoes, "Sistema", Map.of());
  }

  // ── Cabeçalho ────────────────────────────────────────────────────────────

  private void addCabecalho(final Document doc, final int total, final String geradoPorNome) {
    try {
      // Barra azul de identidade
      final PdfPTable banner = new PdfPTable(1);
      banner.setWidthPercentage(100);
      banner.setSpacingAfter(10);
      final PdfPCell bannerCell =
          new PdfPCell(
              new Phrase(
                  "RGM Auto Parts — Relatório de Solicitações",
                  FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.WHITE)));
      bannerCell.setBackgroundColor(HEADER_BG);
      bannerCell.setPadding(10);
      bannerCell.setBorder(Rectangle.NO_BORDER);
      banner.addCell(bannerCell);
      doc.add(banner);

      final String metaLine =
          "Gerado em: "
              + FMT.format(java.time.Instant.now())
              + "   ·   Total: "
              + total
              + " solicitação(ões)"
              + "   ·   Solicitado por: "
              + geradoPorNome;
      final var sub = new Paragraph(metaLine, SUBTITLE_FONT);
      sub.setAlignment(Element.ALIGN_LEFT);
      sub.setSpacingAfter(12);
      doc.add(sub);
    } catch (final Exception e) {
      throw new RuntimeException("Erro ao gerar cabeçalho do PDF", e);
    }
  }

  // ── Tabela principal ─────────────────────────────────────────────────────

  private void addTabela(
      final Document doc,
      final List<Solicitacao> solicitacoes,
      final Map<UUID, String> nomesPorUsuario) {
    try {
      // Colunas: Título | Tipo | Status | Prioridade | Aberto por | Abertura | Encerramento | SLA |
      // Comentário final
      final float[] widths = {5.5f, 2.5f, 3.5f, 2.5f, 4f, 4f, 4f, 2f, 5f};
      final var table = new PdfPTable(widths.length);
      table.setWidthPercentage(100);
      table.setWidths(widths);
      table.setHeaderRows(1);

      addHeaderRow(
          table,
          "Título",
          "Tipo",
          "Status",
          "Prioridade",
          "Aberto por",
          "Abertura",
          "Encerramento",
          "SLA (h)",
          "Comentário final");

      boolean isEven = false;
      for (final Solicitacao s : solicitacoes) {
        final Color bg = isEven ? ROW_ALT : Color.WHITE;

        addCell(table, s.getTitulo(), bg, CELL_BOLD);
        addCell(table, tipoLabel(s.getTipo().name()), bg, CELL_FONT);
        addStatusCell(table, s.getStatus().name(), bg);
        addCell(
            table,
            s.getPrioridade() != null ? prioridadeLabel(s.getPrioridade().name()) : "—",
            bg,
            CELL_FONT);
        addCell(table, nomesPorUsuario.getOrDefault(s.getAbertaPorUsuarioId(), "—"), bg, CELL_FONT);
        addCell(table, s.getCriadaEm() != null ? FMT.format(s.getCriadaEm()) : "—", bg, CELL_FONT);

        // Encerramento: concluída ou cancelada
        final String encerramentoData =
            s.getConcluidaEm() != null
                ? FMT.format(s.getConcluidaEm())
                : s.getCanceladaEm() != null ? FMT.format(s.getCanceladaEm()) : "—";
        addCell(table, encerramentoData, bg, CELL_FONT);

        // SLA em horas (abertura → encerramento)
        final java.time.Instant fim =
            s.getConcluidaEm() != null
                ? s.getConcluidaEm()
                : s.getCanceladaEm() != null ? s.getCanceladaEm() : null;
        final String sla =
            (s.getCriadaEm() != null && fim != null)
                ? String.valueOf(Duration.between(s.getCriadaEm(), fim).toHours())
                : "—";
        addCell(table, sla, bg, CELL_FONT);

        addCell(
            table,
            s.getComentarioFinal() != null && !s.getComentarioFinal().isBlank()
                ? s.getComentarioFinal()
                : "—",
            bg,
            CELL_FONT);

        isEven = !isEven;
      }

      doc.add(table);
    } catch (final Exception e) {
      throw new RuntimeException("Erro ao gerar tabela do PDF", e);
    }
  }

  // ── Células ───────────────────────────────────────────────────────────────

  private void addHeaderRow(final PdfPTable table, final String... headers) {
    for (final String header : headers) {
      final var cell = new PdfPCell(new Phrase(header, HEADER_FONT));
      cell.setBackgroundColor(HEADER_BG);
      cell.setPadding(5);
      cell.setHorizontalAlignment(Element.ALIGN_CENTER);
      cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
      cell.setBorderColor(BORDER_COLOR);
      table.addCell(cell);
    }
  }

  private void addCell(final PdfPTable table, final String text, final Color bg, final Font font) {
    final var cell = new PdfPCell(new Phrase(text != null ? text : "—", font));
    cell.setBackgroundColor(bg);
    cell.setPadding(4);
    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
    cell.setBorderColor(BORDER_COLOR);
    table.addCell(cell);
  }

  private void addStatusCell(final PdfPTable table, final String status, final Color rowBg) {
    final Color textColor =
        switch (status) {
          case "CONCLUIDA" -> new Color(21, 128, 61);
          case "CANCELADA" -> new Color(185, 28, 28);
          case "EM_VALIDACAO" -> new Color(146, 64, 14);
          case "EM_ANDAMENTO" -> new Color(29, 78, 216);
          default -> new Color(60, 60, 60);
        };
    final Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, textColor);
    final var cell = new PdfPCell(new Phrase(statusLabel(status), f));
    cell.setBackgroundColor(rowBg);
    cell.setPadding(4);
    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
    cell.setBorderColor(BORDER_COLOR);
    table.addCell(cell);
  }

  // ── Labels ────────────────────────────────────────────────────────────────

  private String tipoLabel(final String tipo) {
    return switch (tipo) {
      case "REPARO" -> "Reparo";
      case "INSPECAO" -> "Inspeção";
      case "REENGENHARIA" -> "Reengenharia";
      default -> tipo;
    };
  }

  private String statusLabel(final String status) {
    return switch (status) {
      case "A_FAZER" -> "A Fazer";
      case "EM_ANDAMENTO" -> "Em Andamento";
      case "EM_VALIDACAO" -> "Em Validação";
      case "CONCLUIDA" -> "Concluída";
      case "CANCELADA" -> "Cancelada";
      default -> status;
    };
  }

  private String prioridadeLabel(final String prioridade) {
    return switch (prioridade) {
      case "BAIXA" -> "Baixa";
      case "MEDIA" -> "Média";
      case "ALTA" -> "Alta";
      case "URGENTE" -> "Urgente";
      default -> prioridade;
    };
  }
}
