package com.rgm.api.adapter.out.storage;

import com.rgm.api.core.domain.ports.services.StorageService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import java.io.InputStream;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MinioStorageService implements StorageService {

  private final MinioClient minioClient;
  private final String bucketName;
  private final String minioUrl;

  public MinioStorageService(
      final MinioClient minioClient,
      @Value("${minio.bucket.name}") final String bucketName,
      @Value("${minio.url}") final String minioUrl) {
    this.minioClient = minioClient;
    this.bucketName = bucketName;
    this.minioUrl = minioUrl;
  }

  @Override
  public String upload(
      final String fileName, final String contentType, final InputStream content, final long size) {
    final String objectName = UUID.randomUUID() + "/" + fileName;
    try {
      minioClient.putObject(
          PutObjectArgs.builder().bucket(bucketName).object(objectName).stream(content, size, -1)
              .contentType(contentType)
              .build());
    } catch (final Exception e) {
      throw new RuntimeException("Erro ao fazer upload para MinIO: " + e.getMessage(), e);
    }
    return minioUrl + "/" + bucketName + "/" + objectName;
  }
}
