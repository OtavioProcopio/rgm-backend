package com.rgm.api.core.domain.ports.services;

import java.io.InputStream;

/** Porta para upload de arquivos ao bucket (MinIO/S3). Retorna publicUrl persistente. */
public interface StorageService {

  /** Faz upload do arquivo e retorna a publicUrl persistente (sem expiracao). */
  String upload(String fileName, String contentType, InputStream content, long size);
}
