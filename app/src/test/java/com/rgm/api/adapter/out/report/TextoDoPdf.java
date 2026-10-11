package com.rgm.api.adapter.out.report;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.io.IOException;

/** Lê de volta o texto de um PDF gerado, para os testes afirmarem o que o relatório diz. */
final class TextoDoPdf {

  private TextoDoPdf() {}

  static String de(final byte[] pdf) {
    try {
      final PdfReader leitor = new PdfReader(pdf);
      final PdfTextExtractor extrator = new PdfTextExtractor(leitor);
      final StringBuilder texto = new StringBuilder();
      for (int pagina = 1; pagina <= leitor.getNumberOfPages(); pagina++) {
        texto.append(extrator.getTextFromPage(pagina)).append('\n');
      }
      leitor.close();
      return texto.toString().replaceAll("\\s+", " ");
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  static int paginas(final byte[] pdf) {
    try {
      final PdfReader leitor = new PdfReader(pdf);
      final int n = leitor.getNumberOfPages();
      leitor.close();
      return n;
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
