package com.rgm.api.adapter.out.report;

import com.lowagie.text.Chunk;
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
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.EventoModelo;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ModeloPdfService {

  private static final DateTimeFormatter FMT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Sao_Paulo"));
  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.of("America/Sao_Paulo"));

  private static final Color HEADER_BG = new Color(41, 65, 122);
  private static final Color AMBER_BG = new Color(251, 191, 36);
  private static final Color GREEN_BG = new Color(34, 197, 94);
  private static final Color RED_BG = new Color(239, 68, 68);

  private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
  private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);
  private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
  private static final Font HEADER_FONT =
      FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
  private static final Font CELL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8);
  private static final Font CELL_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);

  /** Relatório de lista de modelos (todos os filtrados). */
  public byte[] gerarLista(final List<Modelo> modelos) {
    final var out = new ByteArrayOutputStream();
    final var doc = new Document(PageSize.A4.rotate(), 36, 36, 50, 36);
    PdfWriter.getInstance(doc, out);
    doc.open();
    addTitulo(doc, "RGM Auto Parts — Relatório de Modelos", modelos.size() + " modelos");
    addTabelaLista(doc, modelos);
    doc.close();
    return out.toByteArray();
  }

  /** Ficha completa de um modelo: dados + eventos + solicitações. */
  public byte[] gerarFicha(
      final Modelo modelo,
      final List<EventoModelo> eventos,
      final List<Solicitacao> solicitacoes) {
    final var out = new ByteArrayOutputStream();
    final var doc = new Document(PageSize.A4, 36, 36, 50, 36);
    PdfWriter.getInstance(doc, out);
    doc.open();
    addFichaCabecalho(doc, modelo, solicitacoes);
    if (!eventos.isEmpty()) {
      addSecao(doc, "Eventos do Modelo");
      addTabelaEventos(doc, eventos);
    }
    if (!solicitacoes.isEmpty()) {
      addSecao(doc, "Histórico de Solicitações");
      addTabelaSolicitacoes(doc, solicitacoes);
    }
    doc.close();
    return out.toByteArray();
  }

  // ── helpers ──────────────────────────────────────────────────────────────

  private void addTitulo(final Document doc, final String titulo, final String subtitulo) {
    try {
      final var p = new Paragraph(titulo, TITLE_FONT);
      p.setAlignment(Element.ALIGN_CENTER);
      p.setSpacingAfter(4);
      doc.add(p);
      final var s =
          new Paragraph(
              "Gerado em: " + FMT.format(java.time.Instant.now()) + "   |   " + subtitulo,
              SUBTITLE_FONT);
      s.setAlignment(Element.ALIGN_CENTER);
      s.setSpacingAfter(16);
      doc.add(s);
    } catch (final Exception e) {
      throw new RuntimeException("Erro ao gerar título PDF", e);
    }
  }

  private void addSecao(final Document doc, final String titulo) {
    try {
      final var p = new Paragraph(titulo, SECTION_FONT);
      p.setSpacingBefore(14);
      p.setSpacingAfter(6);
      doc.add(p);
    } catch (final Exception e) {
      throw new RuntimeException("Erro ao gerar seção PDF", e);
    }
  }

  private void addFichaCabecalho(
      final Document doc, final Modelo modelo, final List<Solicitacao> solicitacoes) {
    try {
      final var titulo = new Paragraph(modelo.getCodigo() + " v" + modelo.getVersao(), TITLE_FONT);
      titulo.setSpacingAfter(4);
      doc.add(titulo);

      final var sub = new Paragraph(modelo.getDescricao(), SUBTITLE_FONT);
      sub.setSpacingAfter(2);
      doc.add(sub);

      doc.add(
          new Paragraph(
              "Gerado em: " + FMT.format(java.time.Instant.now()), SUBTITLE_FONT));

      doc.add(Chunk.NEWLINE);

      // KPI summary table
      final long abertas =
          solicitacoes.stream().filter(s -> !s.getStatus().isTerminal()).count();
      final long concluidas =
          solicitacoes.stream()
              .filter(s -> s.getStatus().name().equals("CONCLUIDA"))
              .count();

      final float[] kpiWidths = {2f, 2f, 2f, 2f, 2f};
      final var kpi = new PdfPTable(kpiWidths.length);
      kpi.setWidthPercentage(80);
      kpi.setHorizontalAlignment(Element.ALIGN_LEFT);
      kpi.setWidths(kpiWidths);
      addKpiCell(kpi, "Máquina", modelo.getMaquina(), HEADER_BG);
      addKpiCell(kpi, "Status", modelo.isAtivo() ? "Ativo" : "Inativo",
          modelo.isAtivo() ? GREEN_BG : RED_BG);
      addKpiCell(kpi, "Pendência", modelo.isTemPendenciaAberta() ? "Sim" : "Não",
          modelo.isTemPendenciaAberta() ? AMBER_BG : new Color(156, 163, 175));
      addKpiCell(kpi, "Sol. abertas", String.valueOf(abertas), HEADER_BG);
      addKpiCell(kpi, "Concluídas", String.valueOf(concluidas), new Color(21, 128, 61));
      doc.add(kpi);
    } catch (final Exception e) {
      throw new RuntimeException("Erro ao gerar cabeçalho da ficha", e);
    }
  }

  private void addKpiCell(final PdfPTable table, final String label, final String value,
      final Color bg) {
    final var cell = new PdfPCell();
    cell.setBackgroundColor(bg);
    cell.setPadding(6);
    final var p = new Paragraph();
    p.add(new Chunk(label + "\n", FontFactory.getFont(FontFactory.HELVETICA, 7, Color.WHITE)));
    p.add(new Chunk(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE)));
    cell.addElement(p);
    table.addCell(cell);
  }

  private void addTabelaLista(final Document doc, final List<Modelo> modelos) {
    try {
      final float[] widths = {3f, 2f, 5f, 4f, 2f, 3f, 4f};
      final var table = new PdfPTable(widths.length);
      table.setWidthPercentage(100);
      table.setWidths(widths);

      addHeaderRow(table, "Código", "Versão", "Descrição", "Máquina", "Status", "Pendência",
          "Criado em");

      final Color even = new Color(245, 245, 245);
      boolean isEven = false;
      for (final Modelo m : modelos) {
        final Color bg = isEven ? even : Color.WHITE;
        addCell(table, m.getCodigo(), bg, CELL_BOLD);
        addCell(table, "v" + m.getVersao(), bg, CELL_FONT);
        addCell(table, m.getDescricao(), bg, CELL_FONT);
        addCell(table, m.getMaquina(), bg, CELL_FONT);
        addCell(table, m.isAtivo() ? "Ativo" : "Inativo", bg, CELL_FONT);
        addCell(table, m.isTemPendenciaAberta() ? "⚠ Sim" : "Não", bg, CELL_FONT);
        addCell(table, m.getCriadoEm() != null ? DATE_FMT.format(m.getCriadoEm()) : "—", bg, CELL_FONT);
        isEven = !isEven;
      }
      doc.add(table);
    } catch (final Exception e) {
      throw new RuntimeException("Erro ao gerar tabela de modelos", e);
    }
  }

  private void addTabelaEventos(final Document doc, final List<EventoModelo> eventos) {
    try {
      final float[] widths = {3f, 5f, 5f, 3f};
      final var table = new PdfPTable(widths.length);
      table.setWidthPercentage(100);
      table.setWidths(widths);

      addHeaderRow(table, "Data", "Título", "Descrição", "Tipo");

      final Color even = new Color(245, 245, 245);
      boolean isEven = false;
      for (final EventoModelo e : eventos) {
        final Color bg = isEven ? even : Color.WHITE;
        addCell(table, e.getCriadoEm() != null ? FMT.format(e.getCriadoEm()) : "—", bg, CELL_FONT);
        addCell(table, e.getTitulo(), bg, CELL_FONT);
        addCell(table, e.getDescricao() != null ? e.getDescricao() : "—", bg, CELL_FONT);
        addCell(table, e.getTipo().name(), bg, CELL_FONT);
        isEven = !isEven;
      }
      doc.add(table);
    } catch (final Exception e) {
      throw new RuntimeException("Erro ao gerar tabela de eventos", e);
    }
  }

  private void addTabelaSolicitacoes(final Document doc, final List<Solicitacao> solicitacoes) {
    try {
      final float[] widths = {5f, 3f, 3f, 3f, 4f, 4f};
      final var table = new PdfPTable(widths.length);
      table.setWidthPercentage(100);
      table.setWidths(widths);

      addHeaderRow(table, "Título", "Tipo", "Status", "Prioridade", "Abertura", "Conclusão");

      final Color even = new Color(245, 245, 245);
      boolean isEven = false;
      for (final Solicitacao s : solicitacoes) {
        final Color bg = isEven ? even : Color.WHITE;
        addCell(table, s.getTitulo(), bg, CELL_FONT);
        addCell(table, tipoLabel(s.getTipo().name()), bg, CELL_FONT);
        addCell(table, statusLabel(s.getStatus().name()), bg, CELL_FONT);
        addCell(table, s.getPrioridade() != null ? prioridadeLabel(s.getPrioridade().name()) : "—", bg, CELL_FONT);
        addCell(table, s.getCriadaEm() != null ? FMT.format(s.getCriadaEm()) : "—", bg, CELL_FONT);
        addCell(table, s.getConcluidaEm() != null ? FMT.format(s.getConcluidaEm()) : "—", bg, CELL_FONT);
        isEven = !isEven;
      }
      doc.add(table);
    } catch (final Exception e) {
      throw new RuntimeException("Erro ao gerar tabela de solicitações", e);
    }
  }

  private void addHeaderRow(final PdfPTable table, final String... headers) {
    for (final String header : headers) {
      final var cell = new PdfPCell(new Phrase(header, HEADER_FONT));
      cell.setBackgroundColor(HEADER_BG);
      cell.setPadding(5);
      cell.setHorizontalAlignment(Element.ALIGN_CENTER);
      table.addCell(cell);
    }
  }

  private void addCell(final PdfPTable table, final String text, final Color bg, final Font font) {
    final var cell = new PdfPCell(new Phrase(text, font));
    cell.setBackgroundColor(bg);
    cell.setPadding(4);
    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
    table.addCell(cell);
  }

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
