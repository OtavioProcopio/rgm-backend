package com.rgm.api.adapter.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

  @Bean
  public MinioClient minioClient(
      @Value("${minio.url}") final String url,
      @Value("${minio.access.key}") final String accessKey,
      @Value("${minio.secret.key}") final String secretKey) {
    return MinioClient.builder().endpoint(url).credentials(accessKey, secretKey).build();
  }
}
