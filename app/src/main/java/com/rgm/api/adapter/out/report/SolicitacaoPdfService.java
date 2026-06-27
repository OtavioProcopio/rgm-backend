package com.rgm.api.adapter.out.report;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SolicitacaoPdfService {

  private static final DateTimeFormatter FMT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Sao_Paulo"));

  private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
  private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);
  private static final Font HEADER_FONT =
      FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
  private static final Font CELL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8);

  public byte[] gerar(final List<Solicitacao> solicitacoes) {
    final var out = new ByteArrayOutputStream();
    final var doc = new Document(PageSize.A4.rotate(), 36, 36, 50, 36);
    PdfWriter.getInstance(doc, out);
    doc.open();

    addCabecalho(doc, solicitacoes.size());
    addTabela(doc, solicitacoes);

    doc.close();
    return out.toByteArray();
  }

  private void addCabecalho(final Document doc, final int total) {
    try {
      final var titulo = new Paragraph("RGM Auto Parts — Relatório de Solicitações", TITLE_FONT);
      titulo.setAlignment(Element.ALIGN_CENTER);
      titulo.setSpacingAfter(4);
      doc.add(titulo);

      final var sub =
          new Paragraph(
              "Gerado em: "
                  + FMT.format(java.time.Instant.now())
                  + "   |   Total: "
                  + total
                  + " solicitações",
              SUBTITLE_FONT);
      sub.setAlignment(Element.ALIGN_CENTER);
      sub.setSpacingAfter(14);
      doc.add(sub);
    } catch (final Exception e) {
      throw new RuntimeException("Erro ao gerar cabeçalho do PDF", e);
    }
  }

  private void addTabela(final Document doc, final List<Solicitacao> solicitacoes) {
    try {
      final float[] widths = {3f, 8f, 4f, 4f, 4f, 4f, 5f, 5f, 4f};
      final var table = new PdfPTable(widths.length);
      table.setWidthPercentage(100);
      table.setWidths(widths);

      addHeaderRow(
          table,
          "ID",
          "Título",
          "Tipo",
          "Status",
          "Prioridade",
          "Modelo ID",
          "Criada Em",
          "Concluída Em",
          "SLA (h)");

      final Color even = new Color(245, 245, 245);
      boolean isEven = false;
      for (final Solicitacao s : solicitacoes) {
        final Color bg = isEven ? even : Color.WHITE;
        addCell(table, s.getId().toString().substring(0, 8) + "…", bg);
        addCell(table, s.getTitulo(), bg);
        addCell(table, s.getTipo().name(), bg);
        addCell(table, s.getStatus().name(), bg);
        addCell(table, s.getPrioridade() != null ? s.getPrioridade().name() : "—", bg);
        addCell(table, s.getModeloId().toString().substring(0, 8) + "…", bg);
        addCell(table, s.getCriadaEm() != null ? FMT.format(s.getCriadaEm()) : "—", bg);
        addCell(table, s.getConcluidaEm() != null ? FMT.format(s.getConcluidaEm()) : "—", bg);
        final String sla =
            (s.getCriadaEm() != null && s.getConcluidaEm() != null)
                ? String.valueOf(
                    java.time.Duration.between(s.getCriadaEm(), s.getConcluidaEm()).toHours())
                : "—";
        addCell(table, sla, bg);
        isEven = !isEven;
      }

      doc.add(table);
    } catch (final Exception e) {
      throw new RuntimeException("Erro ao gerar tabela do PDF", e);
    }
  }

  private void addHeaderRow(final PdfPTable table, final String... headers) {
    final Color headerBg = new Color(41, 65, 122);
    for (final String header : headers) {
      final var cell = new PdfPCell(new Phrase(header, HEADER_FONT));
      cell.setBackgroundColor(headerBg);
      cell.setPadding(5);
      cell.setHorizontalAlignment(Element.ALIGN_CENTER);
      table.addCell(cell);
    }
  }

  private void addCell(final PdfPTable table, final String text, final Color bg) {
    final var cell = new PdfPCell(new Phrase(text, CELL_FONT));
    cell.setBackgroundColor(bg);
    cell.setPadding(4);
    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
    table.addCell(cell);
  }
}
