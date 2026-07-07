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
@Table(name = "maquinas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MaquinaJpaEntity {

  @Id private UUID id;

  @Column(nullable = false, unique = true)
  private String nome;

  @Column(nullable = false)
  private boolean ativo;

  @Column(nullable = false)
  private Instant criadoEm;

  @Column(nullable = false)
  private Instant atualizadoEm;
}
