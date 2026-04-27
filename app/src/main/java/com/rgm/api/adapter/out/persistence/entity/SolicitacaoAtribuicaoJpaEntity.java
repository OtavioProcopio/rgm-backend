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
@Table(name = "solicitacao_atribuicoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SolicitacaoAtribuicaoJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private UUID solicitacaoId;

  @Column(nullable = false)
  private UUID usuarioId;

  @Column(nullable = false)
  private UUID atribuidoPorUsuarioId;

  @Column(nullable = false)
  private Instant atribuidoEm;

  private Instant removidoEm;
}
