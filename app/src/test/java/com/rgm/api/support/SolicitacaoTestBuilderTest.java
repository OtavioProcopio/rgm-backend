package com.rgm.api.support;

import static com.rgm.api.support.SolicitacaoTestBuilder.umaSolicitacao;
import static org.assertj.core.api.Assertions.assertThat;

import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SolicitacaoTestBuilderTest {

  @ParameterizedTest
  @EnumSource(StatusSolicitacao.class)
  void shouldBuildValidReparoForEveryStatus(final StatusSolicitacao status) {
    // Act
    final Solicitacao s = umaSolicitacao().noStatus(status).build();

    // Assert
    assertThat(s.getStatus()).isEqualTo(status);
    assertThat(s.getTipo()).isEqualTo(TipoSolicitacao.REPARO);
    assertThat(s.getModeloId()).isNotNull();
    assertThat(s.getPrioridade() != null).isEqualTo(status != StatusSolicitacao.A_FAZER);
  }

  @ParameterizedTest
  @EnumSource(StatusSolicitacao.class)
  void shouldBuildValidCriacaoForEveryStatus(final StatusSolicitacao status) {
    // Act
    final Solicitacao s = umaSolicitacao().doTipoCriacao().noStatus(status).build();

    // Assert
    assertThat(s.getTipo()).isEqualTo(TipoSolicitacao.CRIACAO);
    assertThat(s.getModeloCodigo()).isEqualTo("COD-1");
    assertThat(s.getModeloId() != null).isEqualTo(status == StatusSolicitacao.CONCLUIDA);
  }

  @Test
  void shouldCarryTheValuesTheTestAsked() {
    // Act
    final Solicitacao s =
        umaSolicitacao()
            .emAndamento()
            .comTitulo("Outro")
            .comPrioridade(PrioridadeSolicitacao.URGENTE)
            .comInstrucao("Trocar o gabarito")
            .comVersion(3L)
            .build();

    // Assert
    assertThat(s.getTitulo()).isEqualTo("Outro");
    assertThat(s.getPrioridade()).isEqualTo(PrioridadeSolicitacao.URGENTE);
    assertThat(s.getInstrucao()).isEqualTo("Trocar o gabarito");
    assertThat(s.getVersion()).isEqualTo(3L);
  }
}
