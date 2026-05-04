package com.rgm.api.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "evento_modelo_evidencias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventoModeloEvidenciaJpaEntity {

  @Id private UUID id;

  @Column(nullable = false)
  private UUID eventoModeloId;

  @Column(nullable = false)
  private UUID evidenciaId;
}
