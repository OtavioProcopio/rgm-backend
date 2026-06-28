package com.rgm.api.adapter.out.report;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;

/** Rodapé com número de página e identidade da empresa em todos os PDFs. */
public class PdfFooterEvent extends PdfPageEventHelper {

  private static final Font FOOTER_FONT =
      FontFactory.getFont(FontFactory.HELVETICA, 7, new Color(120, 120, 120));

  private final String geradoPor;
  private PdfTemplate totalPagesTemplate;

  public PdfFooterEvent(final String geradoPor) {
    this.geradoPor = geradoPor;
  }

  @Override
  public void onOpenDocument(final PdfWriter writer, final Document document) {
    totalPagesTemplate = writer.getDirectContent().createTemplate(30, 10);
  }

  @Override
  public void onEndPage(final PdfWriter writer, final Document document) {
    final PdfContentByte cb = writer.getDirectContent();
    final Rectangle pageSize = document.getPageSize();
    final float bottom = document.bottomMargin() - 18f;
    final float left = document.leftMargin();
    final float right = pageSize.getRight() - document.rightMargin();

    // Linha separadora
    cb.setColorStroke(new Color(200, 200, 200));
    cb.setLineWidth(0.5f);
    cb.moveTo(left, bottom + 10);
    cb.lineTo(right, bottom + 10);
    cb.stroke();

    // Esquerda: empresa + gerado por
    final String leftText = "RGM Auto Parts  ·  Gerado por: " + geradoPor;
    cb.beginText();
    cb.setFontAndSize(
        FontFactory.getFont(FontFactory.HELVETICA, 7).getBaseFont(), 7);
    cb.setColorFill(new Color(120, 120, 120));
    cb.setTextMatrix(left, bottom);
    cb.showText(leftText);
    cb.endText();

    // Direita: Página X de Y
    cb.beginText();
    cb.setFontAndSize(
        FontFactory.getFont(FontFactory.HELVETICA, 7).getBaseFont(), 7);
    cb.setColorFill(new Color(120, 120, 120));
    final String pageText = "Página " + writer.getPageNumber() + " de ";
    final float pageTextWidth =
        FontFactory.getFont(FontFactory.HELVETICA, 7).getBaseFont().getWidthPoint(pageText, 7);
    cb.setTextMatrix(right - pageTextWidth - 30, bottom);
    cb.showText(pageText);
    cb.endText();

    // Template para total de páginas (preenchido no onCloseDocument)
    cb.addTemplate(totalPagesTemplate, right - 30, bottom);
  }

  @Override
  public void onCloseDocument(final PdfWriter writer, final Document document) {
    totalPagesTemplate.beginText();
    totalPagesTemplate.setFontAndSize(
        FontFactory.getFont(FontFactory.HELVETICA, 7).getBaseFont(), 7);
    totalPagesTemplate.setColorFill(new Color(120, 120, 120));
    totalPagesTemplate.setTextMatrix(0, 0);
    totalPagesTemplate.showText(String.valueOf(writer.getPageNumber() - 1));
    totalPagesTemplate.endText();
  }
}
