package com.rgm.api.adapter.out.persistence.entity;

import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoAtividadeSolicitacao;
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
@Table(name = "atividades_solicitacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AtividadeSolicitacaoJpaEntity {

  @Id private UUID id;

  @Column(nullable = false)
  private UUID solicitacaoId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TipoAtividadeSolicitacao tipo;

  @Enumerated(EnumType.STRING)
  private StatusSolicitacao deStatus;

  @Enumerated(EnumType.STRING)
  private StatusSolicitacao paraStatus;

  private String comentario;

  @Column(nullable = false)
  private UUID autorUsuarioId;

  @Column(nullable = false)
  private Instant criadaEm;
}
