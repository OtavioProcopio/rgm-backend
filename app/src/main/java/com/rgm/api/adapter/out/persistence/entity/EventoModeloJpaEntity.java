package com.rgm.api.adapter.out.persistence.entity;

import com.rgm.api.core.domain.model.enums.TipoEventoModelo;
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
@Table(name = "eventos_modelo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventoModeloJpaEntity {

  @Id private UUID id;

  @Column(nullable = false)
  private UUID modeloId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TipoEventoModelo tipo;

  @Column(nullable = false)
  private String titulo;

  @Column(nullable = false)
  private String descricao;

  private String estadoModeloDescricao;

  @Column(nullable = false)
  private boolean defineFotoCapa;

  @Column(nullable = false)
  private UUID executadoPorUsuarioId;

  private UUID solicitacaoRelacionadaId;

  @Column(nullable = false)
  private Instant criadoEm;
}
