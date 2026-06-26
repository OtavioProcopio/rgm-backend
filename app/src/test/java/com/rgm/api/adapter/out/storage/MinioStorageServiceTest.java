package com.rgm.api.adapter.out.storage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
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
    assertTrue(url.endsWith("foto.jpg"));
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
  void upload_nomeObjetoContemNomeArquivoOriginal() throws Exception {
    when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
    final InputStream content = new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8));

    final String url = service.upload("documento.pdf", "application/pdf", content, 1L);

    assertTrue(url.contains("documento.pdf"));
  }
}
