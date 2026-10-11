package com.rgm.api.core.domain.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class AssinaturaDeArquivoTest {

  private static byte[] bytes(final String texto) {
    return texto.getBytes(StandardCharsets.ISO_8859_1);
  }

  @Test
  void shouldAcceptJpegWhenHeaderMatches() {
    // Arrange
    final byte[] cabecalho = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};

    // Act
    final boolean confere = AssinaturaDeArquivo.confere("image/jpeg", cabecalho);

    // Assert
    assertTrue(confere);
  }

  @Test
  void shouldAcceptPngWhenHeaderMatches() {
    // Arrange
    final byte[] cabecalho = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

    // Act
    final boolean confere = AssinaturaDeArquivo.confere("IMAGE/PNG", cabecalho);

    // Assert
    assertTrue(confere);
  }

  @Test
  void shouldAcceptGifWhenHeaderMatches() {
    // Arrange
    final byte[] cabecalho = bytes("GIF89a\0\0\0\0\0\0");

    // Act
    final boolean confere = AssinaturaDeArquivo.confere("image/gif", cabecalho);

    // Assert
    assertTrue(confere);
  }

  @Test
  void shouldAcceptWebpWhenHeaderMatches() {
    // Arrange
    final byte[] cabecalho = bytes("RIFF\0\0\0\0WEBP");

    // Act
    final boolean confere = AssinaturaDeArquivo.confere("image/webp", cabecalho);

    // Assert
    assertTrue(confere);
  }

  @Test
  void shouldAcceptPdfWhenHeaderMatches() {
    // Arrange
    final byte[] cabecalho = bytes("%PDF-1.7\n\0\0\0");

    // Act
    final boolean confere = AssinaturaDeArquivo.confere("application/pdf", cabecalho);

    // Assert
    assertTrue(confere);
  }

  @Test
  void shouldAcceptMp4WhenHeaderMatches() {
    // Arrange
    final byte[] cabecalho = bytes("\0\0\0\u0018ftypisom");

    // Act
    final boolean confere = AssinaturaDeArquivo.confere("video/mp4", cabecalho);

    // Assert
    assertTrue(confere);
  }

  @Test
  void shouldRejectWhenContentIsTextButTypeIsPng() {
    // Arrange
    final byte[] cabecalho = bytes("isto e texto");

    // Act
    final boolean confere = AssinaturaDeArquivo.confere("image/png", cabecalho);

    // Assert
    assertFalse(confere);
  }

  @Test
  void shouldRejectWhenHeaderIsShorterThanTheSignature() {
    // Arrange
    final byte[] cabecalho = {(byte) 0xFF, (byte) 0xD8};

    // Act
    final boolean confere = AssinaturaDeArquivo.confere("image/jpeg", cabecalho);

    // Assert
    assertFalse(confere);
  }

  @Test
  void shouldRejectWhenTypeIsUnknownOrNull() {
    // Arrange
    final byte[] cabecalho = bytes("%PDF-1.7\n\0\0\0");

    // Act
    final boolean desconhecido = AssinaturaDeArquivo.confere("text/html", cabecalho);
    final boolean nulo = AssinaturaDeArquivo.confere(null, cabecalho);

    // Assert
    assertFalse(desconhecido);
    assertFalse(nulo);
  }
}
