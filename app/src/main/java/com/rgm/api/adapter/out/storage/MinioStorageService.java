package com.rgm.api.adapter.out.storage;

import com.rgm.api.core.domain.ports.services.StorageService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.InputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
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

  /**
   * O nome do objeto e gerado aqui (UUID + extensao do tipo); o nome enviado pelo cliente nao entra
   * no endereco. O nome original fica so nos dados da evidencia.
   */
  @Override
  public String upload(
      final String fileName, final String contentType, final InputStream content, final long size) {
    final String objectName = UUID.randomUUID() + extensaoDoTipo(contentType);
    try {
      minioClient.putObject(
          PutObjectArgs.builder().bucket(bucketName).object(objectName).stream(content, size, -1)
              .contentType(contentType)
              .build());
    } catch (final Exception e) {
      throw new RuntimeException("Erro ao fazer upload para MinIO: " + e.getMessage(), e);
    }
    return publicUrl + "/" + bucketName + "/" + objectName;
  }

  private static String extensaoDoTipo(final String contentType) {
    if (contentType == null) {
      return "";
    }
    return switch (contentType.toLowerCase(Locale.ROOT)) {
      case "image/jpeg" -> ".jpg";
      case "image/png" -> ".png";
      case "image/gif" -> ".gif";
      case "image/webp" -> ".webp";
      case "application/pdf" -> ".pdf";
      case "video/mp4" -> ".mp4";
      default -> "";
    };
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
  public byte[] baixar(final String fileUrl) {
    final String objectName = objectNameDe(fileUrl);
    try (var in =
        minioClient.getObject(
            GetObjectArgs.builder().bucket(bucketName).object(objectName).build())) {
      return in.readAllBytes();
    } catch (final Exception e) {
      throw new RuntimeException("Erro ao baixar do MinIO: " + e.getMessage(), e);
    }
  }

  private String objectNameDe(final String fileUrl) {
    final String prefix = "/" + bucketName + "/";
    final int idx = fileUrl == null ? -1 : fileUrl.indexOf(prefix);
    if (idx == -1) {
      throw new IllegalArgumentException("URL fora do bucket: " + fileUrl);
    }
    return decodeFileNameSegment(fileUrl.substring(idx + prefix.length()));
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
