package com.rgm.api.core.domain.validation;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Confere os primeiros bytes de um arquivo contra o tipo que o cliente declarou. So conhece os
 * tipos aceitos como evidencia; qualquer outro nao confere.
 */
public final class AssinaturaDeArquivo {

  /** Quantidade de bytes do inicio do arquivo que basta para decidir. */
  public static final int TAMANHO_DO_CABECALHO = 12;

  private AssinaturaDeArquivo() {}

  public static boolean confere(final String mimeType, final byte[] cabecalho) {
    if (mimeType == null || cabecalho == null) {
      return false;
    }
    return switch (mimeType.toLowerCase(Locale.ROOT)) {
      case "image/jpeg" -> comeca(cabecalho, 0, (byte) 0xFF, (byte) 0xD8, (byte) 0xFF);
      case "image/png" -> comeca(
          cabecalho,
          0,
          (byte) 0x89,
          (byte) 0x50,
          (byte) 0x4E,
          (byte) 0x47,
          (byte) 0x0D,
          (byte) 0x0A,
          (byte) 0x1A,
          (byte) 0x0A);
      case "image/gif" -> texto(cabecalho, 0, "GIF87a") || texto(cabecalho, 0, "GIF89a");
      case "image/webp" -> texto(cabecalho, 0, "RIFF") && texto(cabecalho, 8, "WEBP");
      case "application/pdf" -> texto(cabecalho, 0, "%PDF");
      case "video/mp4" -> texto(cabecalho, 4, "ftyp");
      default -> false;
    };
  }

  private static boolean comeca(final byte[] dados, final int inicio, final byte... esperado) {
    if (dados.length < inicio + esperado.length) {
      return false;
    }
    for (int i = 0; i < esperado.length; i++) {
      if (dados[inicio + i] != esperado[i]) {
        return false;
      }
    }
    return true;
  }

  private static boolean texto(final byte[] dados, final int inicio, final String esperado) {
    return comeca(dados, inicio, esperado.getBytes(StandardCharsets.US_ASCII));
  }
}
