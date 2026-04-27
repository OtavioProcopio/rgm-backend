package com.rgm.api.adapter.out.persistence.entity;

import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "solicitacoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SolicitacaoJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private String titulo;

  private String descricao;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TipoSolicitacao tipo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private StatusSolicitacao status;

  @Enumerated(EnumType.STRING)
  private PrioridadeSolicitacao prioridade;

  @Column(nullable = false)
  private UUID modeloId;

  @Column(nullable = false)
  private UUID abertaPorUsuarioId;

  private String comentarioFinal;

  @Column(nullable = false)
  private Instant criadaEm;

  @Column(nullable = false)
  private Instant atualizadaEm;

  private Instant concluidaEm;

  private Instant canceladaEm;
}
