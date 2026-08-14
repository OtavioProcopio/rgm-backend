package com.rgm.api.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fotos_galeria_modelo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FotoGaleriaModeloJpaEntity {

  @Id private UUID id;

  @Column(nullable = false)
  private UUID modeloId;

  @Column(nullable = false)
  private String publicUrl;

  @Column(nullable = false)
  private String identificacao;

  @Column(nullable = false)
  private boolean principal;

  private UUID enviadaPorUsuarioId;

  @Column(nullable = false)
  private Instant criadoEm;
}
