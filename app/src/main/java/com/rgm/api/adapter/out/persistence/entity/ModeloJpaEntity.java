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
@Table(name = "modelos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ModeloJpaEntity {

  @Id private UUID id;

  @Column(nullable = false)
  private String codigo;

  @Column(nullable = false)
  private int versao;

  @Column(nullable = false)
  private String descricao;

  private String observacoes;

  private String fotoUrl;

  private Instant fotoAtualizadaEm;

  private String estadoAtualDescricao;

  private Instant estadoAtualAtualizadoEm;

  @Column(nullable = false)
  private boolean ativo;

  @Column(nullable = false)
  private UUID maquinaId;

  @Column(nullable = false)
  private boolean temPendenciaAberta;

  @Column(nullable = false)
  private Instant criadoEm;

  @Column(nullable = false)
  private Instant atualizadoEm;
}
