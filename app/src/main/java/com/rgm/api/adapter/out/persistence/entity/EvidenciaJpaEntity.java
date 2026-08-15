package com.rgm.api.adapter.out.persistence.entity;

import com.rgm.api.core.domain.model.enums.TipoEvidencia;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

  @Id private UUID id;

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

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private TipoEvidencia tipo;

  @Column(columnDefinition = "TEXT")
  private String descricao;
}
