package com.rgm.api.adapter.out.storage;

import com.rgm.api.core.domain.ports.services.StorageService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.InputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MinioStorageService implements StorageService {

  private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

  private final MinioClient minioClient;
  private final String bucketName;
  private final String publicUrl;

  public MinioStorageService(
      final MinioClient minioClient,
      @Value("${minio.bucket.name}") final String bucketName,
      @Value("${minio.public-url:${minio.url}}") final String publicUrl) {
    this.minioClient = minioClient;
    this.bucketName = bucketName;
    this.publicUrl = publicUrl;
  }

  @Override
  public String upload(
      final String fileName, final String contentType, final InputStream content, final long size) {
    final String uuid = UUID.randomUUID().toString();
    final String objectName = uuid + "/" + fileName;
    try {
      minioClient.putObject(
          PutObjectArgs.builder().bucket(bucketName).object(objectName).stream(content, size, -1)
              .contentType(contentType)
              .build());
    } catch (final Exception e) {
      throw new RuntimeException("Erro ao fazer upload para MinIO: " + e.getMessage(), e);
    }
    return publicUrl + "/" + bucketName + "/" + uuid + "/" + encodeSegment(fileName);
  }

  private String encodeSegment(final String segment) {
    return URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20");
  }

  private String decodeFileNameSegment(final String objectName) {
    final int sepIdx = objectName.indexOf('/');
    if (sepIdx == -1) {
      return URLDecoder.decode(objectName, StandardCharsets.UTF_8);
    }
    final String uuidPart = objectName.substring(0, sepIdx);
    final String fileNamePart =
        URLDecoder.decode(objectName.substring(sepIdx + 1), StandardCharsets.UTF_8);
    return uuidPart + "/" + fileNamePart;
  }

  @Override
  public void delete(final String fileUrl) {
    if (fileUrl == null || fileUrl.isBlank()) {
      return;
    }
    final String prefix = "/" + bucketName + "/";
    final int idx = fileUrl.indexOf(prefix);
    if (idx == -1) {
      return;
    }
    final String encodedObjectName = fileUrl.substring(idx + prefix.length());
    final String objectName = decodeFileNameSegment(encodedObjectName);
    try {
      minioClient.removeObject(
          RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
    } catch (final Exception e) {
      log.error("Erro ao deletar objeto no MinIO: {}", objectName, e);
    }
  }
}
