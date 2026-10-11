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
}
