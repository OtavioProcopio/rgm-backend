package com.rgm.api.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "evidencias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EvidenciaJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private String publicUrl;

  @Column(nullable = false)
  private String mimeType;

  @Column(nullable = false)
  private String nomeArquivo;

  private Integer tamanhoBytes;

  @Column(nullable = false)
  private UUID enviadaPorUsuarioId;

  @Column(nullable = false)
  private Instant criadaEm;
}
