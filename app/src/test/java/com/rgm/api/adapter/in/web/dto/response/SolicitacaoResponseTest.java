package com.rgm.api.adapter.in.web.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.rgm.api.core.application.usecases.solicitacao.ObterSolicitacaoUseCase.ResponsavelNome;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.enums.AcaoSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SolicitacaoResponseTest {

  private static Solicitacao solicitacao() {
    return Solicitacao.abrir(
        "Titulo",
        "Desc",
        TipoSolicitacao.REPARO,
        UUID.randomUUID(),
        UUID.randomUUID(),
        Instant.now());
  }

  @Test
  void shouldFillNamesAndKeepIdsWhenBuiltWithNames() {
    // Arrange
    final Solicitacao sol = solicitacao();
    final UUID brunoId = UUID.randomUUID();
    final UUID carlaId = UUID.randomUUID();

    // Act
    final SolicitacaoResponse response =
        SolicitacaoResponse.from(
            sol,
            List.of(brunoId, carlaId),
            EnumSet.of(AcaoSolicitacao.EDITAR),
            List.of(new ResponsavelNome(brunoId, "Bruno"), new ResponsavelNome(carlaId, null)),
            "Ana");

    // Assert
    assertThat(response.responsavelIds()).containsExactly(brunoId, carlaId);
    assertThat(response.abertaPorUsuarioId()).isEqualTo(sol.getAbertaPorUsuarioId());
    assertThat(response.responsaveis())
        .containsExactly(
            new ResponsavelResponse(brunoId, "Bruno"), new ResponsavelResponse(carlaId, null));
    assertThat(response.abertaPorNome()).isEqualTo("Ana");
    assertThat(response.acoesPermitidas()).containsExactly("EDITAR");
  }

  @Test
  void shouldKeepEmptyListWhenBuiltWithEmptyResponsaveis() {
    // Arrange
    final Solicitacao sol = solicitacao();

    // Act
    final SolicitacaoResponse response =
        SolicitacaoResponse.from(sol, List.of(), null, List.of(), null);

    // Assert
    assertThat(response.responsaveis()).isEmpty();
    assertThat(response.abertaPorNome()).isNull();
  }

  @Test
  void shouldLeaveNamesNullWhenBuiltWithoutNames() {
    // Arrange
    final Solicitacao sol = solicitacao();
    final UUID responsavelId = UUID.randomUUID();

    // Act
    final SolicitacaoResponse soSolicitacao = SolicitacaoResponse.from(sol);
    final SolicitacaoResponse comIds = SolicitacaoResponse.from(sol, List.of(responsavelId));

    // Assert
    assertThat(soSolicitacao.responsaveis()).isNull();
    assertThat(soSolicitacao.abertaPorNome()).isNull();
    assertThat(comIds.responsaveis()).isNull();
    assertThat(comIds.abertaPorNome()).isNull();
    assertThat(comIds.responsavelIds()).containsExactly(responsavelId);
    assertThat(comIds.abertaPorUsuarioId()).isEqualTo(sol.getAbertaPorUsuarioId());
  }

  @Test
  void shouldCarryIdAndNameWhenMappingResponsavel() {
    // Arrange
    final UUID id = UUID.randomUUID();

    // Act
    final ResponsavelResponse response = ResponsavelResponse.from(new ResponsavelNome(id, "Bruno"));

    // Assert
    assertThat(response.id()).isEqualTo(id);
    assertThat(response.nome()).isEqualTo("Bruno");
  }
}
