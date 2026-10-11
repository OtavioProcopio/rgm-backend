package com.rgm.api.adapter.out.storage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MinioStorageServiceTest {

  private MinioClient minioClient;
  private MinioStorageService service;

  @BeforeEach
  void setUp() {
    minioClient = mock(MinioClient.class);
    service = new MinioStorageService(minioClient, "test-bucket", "http://localhost:9000");
  }

  @Test
  void upload_retornaUrlPublicaComBucketEObjeto() throws Exception {
    final InputStream content = new ByteArrayInputStream("dados".getBytes(StandardCharsets.UTF_8));
    when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

    final String url = service.upload("foto.jpg", "image/jpeg", content, 5L);

    assertTrue(url.startsWith("http://localhost:9000/test-bucket/"));
    assertTrue(url.endsWith(".jpg"));
  }

  @Test
  void upload_lancaRuntimeExceptionQuandoMinioFalha() throws Exception {
    final InputStream content = new ByteArrayInputStream("dados".getBytes(StandardCharsets.UTF_8));
    when(minioClient.putObject(any(PutObjectArgs.class)))
        .thenThrow(new RuntimeException("minio down"));

    final RuntimeException ex =
        assertThrows(
            RuntimeException.class, () -> service.upload("x.jpg", "image/jpeg", content, 1L));

    assertTrue(ex.getMessage().contains("MinIO"));
  }

  @Test
  void shouldNotPutOriginalFileNameInObjectNameWhenUploading() throws Exception {
    // Arrange
    when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
    final InputStream content = new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8));

    // Act
    final String url = service.upload("../../laudo final.pdf", "application/pdf", content, 1L);

    // Assert
    assertFalse(url.contains("laudo"));
    assertTrue(url.matches("http://localhost:9000/test-bucket/[0-9a-f-]{36}\\.pdf"));
  }

  @Test
  void shouldUseNoExtensionWhenTypeIsUnknown() throws Exception {
    // Arrange
    when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
    final InputStream content = new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8));

    // Act
    final String url = service.upload("x.bin", "application/octet-stream", content, 1L);

    // Assert
    assertTrue(url.matches("http://localhost:9000/test-bucket/[0-9a-f-]{36}"));
  }

  @Test
  void delete_comUrlInvalidaNaoFazNada() throws Exception {
    service.delete(null);
    service.delete(" ");
    service.delete("http://localhost:9000/wrong-bucket/file.jpg");
    verifyNoInteractions(minioClient);
  }

  @Test
  void delete_comUrlValidaChamaRemoveObject() throws Exception {
    service.delete("http://localhost:9000/test-bucket/uuid-123/foto.jpg");
    verify(minioClient).removeObject(any(io.minio.RemoveObjectArgs.class));
  }

  @Test
  void delete_decodificaNomeDeArquivoComEspacosECaracteresEspeciais() throws Exception {
    service.delete("http://localhost:9000/test-bucket/uuid-123/foto%20de%20teste%20%C3%A9.jpg");

    verify(minioClient)
        .removeObject(
            argThat(
                (RemoveObjectArgs args) -> args.object().equals("uuid-123/foto de teste é.jpg")));
  }

  @Test
  void delete_quandoMinioFalhaCapturaExcecao() throws Exception {
    doThrow(new RuntimeException("minio error"))
        .when(minioClient)
        .removeObject(any(io.minio.RemoveObjectArgs.class));

    assertDoesNotThrow(() -> service.delete("http://localhost:9000/test-bucket/uuid-123/foto.jpg"));
  }

  @Test
  void shouldReturnObjectBytesWhenDownloadingByPublicUrl() throws Exception {
    // Arrange
    final var resposta = mock(io.minio.GetObjectResponse.class);
    when(resposta.readAllBytes()).thenReturn(new byte[] {7, 8});
    when(minioClient.getObject(any(io.minio.GetObjectArgs.class))).thenReturn(resposta);

    // Act
    final byte[] bytes = service.baixar("http://localhost:9000/test-bucket/abc-123.png");

    // Assert
    assertEquals(2, bytes.length);
    verify(minioClient)
        .getObject(argThat((io.minio.GetObjectArgs a) -> "abc-123.png".equals(a.object())));
  }

  @Test
  void shouldFailWhenTheUrlIsOutsideTheBucket() {
    // Arrange
    final String url = "http://outro/host/arquivo.png";

    // Act
    final var acao = (org.junit.jupiter.api.function.Executable) () -> service.baixar(url);

    // Assert
    assertThrows(IllegalArgumentException.class, acao);
  }

  @Test
  void shouldWrapTheFailureWhenTheDownloadBreaks() throws Exception {
    // Arrange
    when(minioClient.getObject(any(io.minio.GetObjectArgs.class)))
        .thenThrow(new RuntimeException("minio fora"));

    // Act
    final RuntimeException erro =
        assertThrows(
            RuntimeException.class,
            () -> service.baixar("http://localhost:9000/test-bucket/abc.png"));

    // Assert
    assertTrue(erro.getMessage().contains("MinIO"));
  }
}
