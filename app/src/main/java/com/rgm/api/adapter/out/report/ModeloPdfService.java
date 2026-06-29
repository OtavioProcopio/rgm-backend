package com.rgm.api.adapter.out.report;

import com.lowagie.text.Chunk;
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
import com.rgm.api.core.domain.model.aggregates.EventoModelo;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ModeloPdfService {

  private static final DateTimeFormatter FMT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Sao_Paulo"));
  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.of("America/Sao_Paulo"));

  private static final Color HEADER_BG = new Color(41, 65, 122);
  private static final Color AMBER_BG = new Color(180, 120, 20);
  private static final Color GREEN_BG = new Color(21, 128, 61);
  private static final Color RED_BG = new Color(185, 28, 28);
  private static final Color ROW_ALT = new Color(245, 247, 250);
  private static final Color BORDER_COLOR = new Color(220, 220, 220);

  private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15);
  private static final Font SUBTITLE_FONT =
      FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(80, 80, 80));
  private static final Font SECTION_FONT =
      FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, HEADER_BG);
  private static final Font HEADER_FONT =
      FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
  private static final Font CELL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 7);
  private static final Font CELL_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7);
  private static final Font CELL_SMALL =
      FontFactory.getFont(FontFactory.HELVETICA, 6, new Color(80, 80, 80));

  // ── API pública ───────────────────────────────────────────────────────────

  /** Relatório de lista de modelos. */
  public byte[] gerarLista(final List<Modelo> modelos, final String geradoPorNome) {
    final var out = new ByteArrayOutputStream();
    final var doc = new Document(PageSize.A4.rotate(), 40, 40, 55, 45);
    final PdfWriter writer = PdfWriter.getInstance(doc, out);
    writer.setPageEvent(new PdfFooterEvent(geradoPorNome));
    doc.open();
    addBanner(doc, "RGM Auto Parts — Relatório de Modelos");
    addMeta(doc, modelos.size() + " modelo(s)", geradoPorNome);
    addTabelaLista(doc, modelos);
    doc.close();
    return out.toByteArray();
  }

  /** Mantém compatibilidade sem nome do solicitante. */
  public byte[] gerarLista(final List<Modelo> modelos) {
    return gerarLista(modelos, "Sistema");
  }

  /** Ficha completa de um modelo: dados + eventos + solicitações com histórico. */
  public byte[] gerarFicha(
      final Modelo modelo,
      final List<EventoModelo> eventos,
      final List<Solicitacao> solicitacoes,
      final Map<UUID, List<AtividadeSolicitacao>> atividadesPorSolicitacao,
      final String geradoPorNome,
      final Map<UUID, String> nomesPorUsuario) {
    final var out = new ByteArrayOutputStream();
    final var doc = new Document(PageSize.A4, 40, 40, 55, 45);
    final PdfWriter writer = PdfWriter.getInstance(doc, out);
    writer.setPageEvent(new PdfFooterEvent(geradoPorNome));
    doc.open();
    addFichaCabecalho(doc, modelo, solicitacoes, geradoPorNome);
    if (!eventos.isEmpty()) {
      addSecao(doc, "Eventos do Modelo");
      addTabelaEventos(doc, eventos, nomesPorUsuario);
    }
    if (!solicitacoes.isEmpty()) {
      addSecao(doc, "Histórico de Solicitações");
      for (final Solicitacao s : solicitacoes) {
        addSolicitacaoComHistorico(
            doc, s, atividadesPorSolicitacao.getOrDefault(s.getId(), List.of()), nomesPorUsuario);
      }
    }
    doc.close();
    return out.toByteArray();
  }

  /** Mantém compatibilidade sem resolução de usuários. */
  public byte[] gerarFicha(
      final Modelo modelo,
      final List<EventoModelo> eventos,
      final List<Solicitacao> solicitacoes,
      final Map<UUID, List<AtividadeSolicitacao>> atividadesPorSolicitacao) {
    return gerarFicha(modelo, eventos, solicitacoes, atividadesPorSolicitacao, "Sistema", Map.of());
  }

  // ── Cabeçalho / Banner ───────────────────────────────────────────────────

  private void addBanner(final Document doc, final String titulo) {
    try {
      final PdfPTable banner = new PdfPTable(1);
      banner.setWidthPercentage(100);
      banner.setSpacingAfter(8);
      final PdfPCell cell =
          new PdfPCell(
              new Phrase(titulo, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.WHITE)));
      cell.setBackgroundColor(HEADER_BG);
      cell.setPadding(10);
      cell.setBorder(Rectangle.NO_BORDER);
      banner.addCell(cell);
      doc.add(banner);
    } catch (final Exception e) {
      throw new RuntimeException("Erro ao gerar banner PDF", e);
    }
  }

  private void addMeta(final Document doc, final String extra, final String geradoPorNome) {
    try {
      final var p =
          new Paragraph(
              "Gerado em: "
                  + FMT.format(java.time.Instant.now())
                  + "   ·   "
                  + extra
                  + "   ·   Solicitado por: "
                  + geradoPorNome,
              SUBTITLE_FONT);
      p.setSpacingAfter(12);
      doc.add(p);
    } catch (final Exception e) {
      throw new RuntimeException("Erro ao gerar meta PDF", e);
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
      final Document doc,
      final Modelo modelo,
      final List<Solicitacao> solicitacoes,
      final String geradoPorNome) {
    try {
      addBanner(
          doc, modelo.getCodigo() + "  v" + modelo.getVersao() + "  —  " + modelo.getDescricao());

      final var meta =
          new Paragraph(
              "Máquina: "
                  + modelo.getMaquina()
                  + "   ·   Gerado em: "
                  + FMT.format(java.time.Instant.now())
                  + "   ·   Solicitado por: "
                  + geradoPorNome,
              SUBTITLE_FONT);
      meta.setSpacingAfter(6);
      doc.add(meta);

      if (modelo.getEstadoAtualDescricao() != null && !modelo.getEstadoAtualDescricao().isBlank()) {
        final var estado =
            new Paragraph(
                "Estado atual: " + modelo.getEstadoAtualDescricao(),
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, new Color(80, 80, 80)));
        estado.setSpacingAfter(6);
        doc.add(estado);
      }

      if (modelo.getObservacoes() != null && !modelo.getObservacoes().isBlank()) {
        final var obs =
            new Paragraph(
                "Observações: " + modelo.getObservacoes(),
                FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(80, 80, 80)));
        obs.setSpacingAfter(8);
        doc.add(obs);
      }

      // KPI cards
      final long abertas = solicitacoes.stream().filter(s -> !s.getStatus().isTerminal()).count();
      final long concluidas =
          solicitacoes.stream().filter(s -> "CONCLUIDA".equals(s.getStatus().name())).count();
      final long canceladas =
          solicitacoes.stream().filter(s -> "CANCELADA".equals(s.getStatus().name())).count();

      final float[] kpiWidths = {2f, 2f, 2f, 2f, 2f, 2f};
      final var kpi = new PdfPTable(kpiWidths.length);
      kpi.setWidthPercentage(100);
      kpi.setWidths(kpiWidths);
      kpi.setSpacingAfter(10);
      addKpiCell(
          kpi,
          "Status",
          modelo.isAtivo() ? "Ativo" : "Inativo",
          modelo.isAtivo() ? GREEN_BG : RED_BG);
      addKpiCell(
          kpi,
          "Pendência",
          modelo.isTemPendenciaAberta() ? "⚠ Sim" : "Não",
          modelo.isTemPendenciaAberta() ? AMBER_BG : new Color(100, 100, 100));
      addKpiCell(kpi, "Total solicitações", String.valueOf(solicitacoes.size()), HEADER_BG);
      addKpiCell(kpi, "Em aberto", String.valueOf(abertas), new Color(29, 78, 216));
      addKpiCell(kpi, "Concluídas", String.valueOf(concluidas), GREEN_BG);
      addKpiCell(kpi, "Canceladas", String.valueOf(canceladas), new Color(100, 100, 100));
      doc.add(kpi);
    } catch (final Exception e) {
      throw new RuntimeException("Erro ao gerar cabeçalho da ficha", e);
    }
  }

  private void addKpiCell(
      final PdfPTable table, final String label, final String value, final Color bg) {
    final var cell = new PdfPCell();
    cell.setBackgroundColor(bg);
    cell.setPadding(7);
    cell.setBorder(Rectangle.NO_BORDER);
    final var p = new Paragraph();
    p.add(new Chunk(label + "\n", FontFactory.getFont(FontFactory.HELVETICA, 7, Color.WHITE)));
    p.add(new Chunk(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE)));
    cell.addElement(p);
    table.addCell(cell);
  }

  // ── Tabela de lista de modelos ────────────────────────────────────────────

  private void addTabelaLista(final Document doc, final List<Modelo> modelos) {
    try {
      // Código | Versão | Máquina | Descrição | Estado atual | Status | Pendência | Criado em
      final float[] widths = {3f, 1.5f, 3.5f, 5f, 4f, 2f, 2.5f, 3f};
      final var table = new PdfPTable(widths.length);
      table.setWidthPercentage(100);
      table.setWidths(widths);
      table.setHeaderRows(1);

      addHeaderRow(
          table,
          "Código",
          "Versão",
          "Máquina",
          "Descrição",
          "Estado atual",
          "Status",
          "Pendência",
          "Criado em");

      boolean isEven = false;
      for (final Modelo m : modelos) {
        final Color bg = isEven ? ROW_ALT : Color.WHITE;
        addCell(table, m.getCodigo(), bg, CELL_BOLD);
        addCell(table, "v" + m.getVersao(), bg, CELL_FONT);
        addCell(table, m.getMaquina(), bg, CELL_FONT);
        addCell(table, m.getDescricao(), bg, CELL_FONT);
        addCell(
            table,
            m.getEstadoAtualDescricao() != null && !m.getEstadoAtualDescricao().isBlank()
                ? m.getEstadoAtualDescricao()
                : "—",
            bg,
            CELL_FONT);
        addCell(table, m.isAtivo() ? "Ativo" : "Inativo", bg, CELL_FONT);
        addCell(table, m.isTemPendenciaAberta() ? "⚠ Sim" : "Não", bg, CELL_FONT);
        addCell(
            table, m.getCriadoEm() != null ? DATE_FMT.format(m.getCriadoEm()) : "—", bg, CELL_FONT);
        isEven = !isEven;
      }
      doc.add(table);
    } catch (final Exception e) {
      throw new RuntimeException("Erro ao gerar tabela de modelos", e);
    }
  }

  // ── Tabela de eventos ─────────────────────────────────────────────────────

  private void addTabelaEventos(
      final Document doc,
      final List<EventoModelo> eventos,
      final Map<UUID, String> nomesPorUsuario) {
    try {
      // Data | Tipo | Título | Descrição | Executado por
      final float[] widths = {3f, 3f, 4f, 5f, 3.5f};
      final var table = new PdfPTable(widths.length);
      table.setWidthPercentage(100);
      table.setWidths(widths);
      table.setHeaderRows(1);

      addHeaderRow(table, "Data", "Tipo", "Título", "Descrição", "Executado por");

      boolean isEven = false;
      for (final EventoModelo e : eventos) {
        final Color bg = isEven ? ROW_ALT : Color.WHITE;
        addCell(table, e.getCriadoEm() != null ? FMT.format(e.getCriadoEm()) : "—", bg, CELL_FONT);
        addCell(table, tipoEventoLabel(e.getTipo().name()), bg, CELL_FONT);
        addCell(table, e.getTitulo(), bg, CELL_BOLD);
        addCell(table, e.getDescricao() != null ? e.getDescricao() : "—", bg, CELL_FONT);
        addCell(
            table, nomesPorUsuario.getOrDefault(e.getExecutadoPorUsuarioId(), "—"), bg, CELL_FONT);
        isEven = !isEven;
      }
      doc.add(table);
    } catch (final Exception e) {
      throw new RuntimeException("Erro ao gerar tabela de eventos", e);
    }
  }

  // ── Histórico de solicitação ──────────────────────────────────────────────

  private void addSolicitacaoComHistorico(
      final Document doc,
      final Solicitacao s,
      final List<AtividadeSolicitacao> atividades,
      final Map<UUID, String> nomesPorUsuario) {
    try {
      final var header = new Paragraph();
      header.setSpacingBefore(10);
      header.add(new Chunk(s.getTitulo() + "  ", CELL_BOLD));
      final String meta =
          tipoLabel(s.getTipo().name())
              + " · "
              + statusLabel(s.getStatus().name())
              + (s.getPrioridade() != null ? " · " + prioridadeLabel(s.getPrioridade().name()) : "")
              + "   Aberta: "
              + (s.getCriadaEm() != null ? FMT.format(s.getCriadaEm()) : "—")
              + "   Aberto por: "
              + nomesPorUsuario.getOrDefault(s.getAbertaPorUsuarioId(), "—")
              + (s.getConcluidaEm() != null
                  ? "   Concluída: " + FMT.format(s.getConcluidaEm())
                  : "")
              + (s.getCanceladaEm() != null
                  ? "   Cancelada: " + FMT.format(s.getCanceladaEm())
                  : "");
      header.add(
          new Chunk(meta, FontFactory.getFont(FontFactory.HELVETICA, 7, new Color(80, 80, 80))));
      doc.add(header);

      if (s.getDescricao() != null && !s.getDescricao().isBlank()) {
        final var desc =
            new Paragraph(
                "Descrição: " + s.getDescricao(),
                FontFactory.getFont(FontFactory.HELVETICA, 7, new Color(80, 80, 80)));
        desc.setSpacingAfter(2);
        doc.add(desc);
      }

      if (s.getComentarioFinal() != null && !s.getComentarioFinal().isBlank()) {
        final var comentFinal =
            new Paragraph(
                "Comentário final: " + s.getComentarioFinal(),
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7, new Color(80, 80, 80)));
        comentFinal.setSpacingAfter(4);
        doc.add(comentFinal);
      }

      if (atividades.isEmpty()) {
        doc.add(
            new Paragraph(
                "  (sem registros de atividade)",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7, Color.GRAY)));
        return;
      }

      // Data | Tipo | Responsável | Mudança de status | Comentário / Detalhe
      final float[] widths = {3f, 3f, 3.5f, 3.5f, 8f};
      final var table = new PdfPTable(widths.length);
      table.setWidthPercentage(100);
      table.setWidths(widths);
      table.setHeaderRows(1);
      addHeaderRow(
          table, "Data", "Tipo", "Responsável", "Mudança de status", "Comentário / Detalhe");

      boolean isEven = false;
      for (final AtividadeSolicitacao a : atividades) {
        final Color bg = isEven ? ROW_ALT : Color.WHITE;
        addCell(table, a.getCriadaEm() != null ? FMT.format(a.getCriadaEm()) : "—", bg, CELL_FONT);
        addCell(table, tipoAtividadeLabel(a.getTipo().name()), bg, CELL_FONT);
        addCell(table, nomesPorUsuario.getOrDefault(a.getAutorUsuarioId(), "—"), bg, CELL_FONT);
        final String mudanca =
            (a.getDeStatus() != null && a.getParaStatus() != null)
                ? statusLabel(a.getDeStatus().name())
                    + " → "
                    + statusLabel(a.getParaStatus().name())
                : "—";
        addCell(table, mudanca, bg, CELL_FONT);
        addCell(table, a.getComentario() != null ? a.getComentario() : "—", bg, CELL_FONT);
        isEven = !isEven;
      }
      doc.add(table);
    } catch (final Exception e) {
      throw new RuntimeException("Erro ao gerar histórico de solicitação no PDF", e);
    }
  }

  // ── Helpers de células ────────────────────────────────────────────────────

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

  private String tipoEventoLabel(final String tipo) {
    return switch (tipo) {
      case "MODIFICACAO" -> "Modificação";
      case "INSPECAO" -> "Inspeção";
      case "REPARO" -> "Reparo";
      case "AJUSTE" -> "Ajuste";
      case "MANUTENCAO" -> "Manutenção";
      case "OUTRO" -> "Outro";
      default -> tipo;
    };
  }

  private String tipoAtividadeLabel(final String tipo) {
    return switch (tipo) {
      case "ABERTURA" -> "Abertura";
      case "ATRIBUICAO" -> "Atribuição";
      case "MUDANCA_STATUS" -> "Mudança de status";
      case "COMENTARIO" -> "Comentário";
      case "EVIDENCIA_ADICIONADA" -> "Evidência";
      default -> tipo;
    };
  }
}
