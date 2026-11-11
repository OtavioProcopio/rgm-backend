package com.rgm.api.adapter.in.web.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.rgm.api.core.domain.ports.repositories.ContagensDeModelos;
import com.rgm.api.core.domain.ports.repositories.PosicaoDoModelo;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContagensDeModelosResponseTest {

  @Test
  void shouldKeepTheTotalAndTheOrderOfTheCountsNamedByTipoAndMaquina() {
    // Arrange
    final var contagens =
        new ContagensDeModelos(
            7,
            List.of(
                new ContagensDeModelos.Quantidade("RESINA", 4),
                new ContagensDeModelos.Quantidade("COQUILHA_ACO", 3)),
            List.of(new ContagensDeModelos.Quantidade("FBOX", 7)));

    // Act
    final var response = ContagensDeModelosResponse.from(contagens);

    // Assert
    assertThat(response.total()).isEqualTo(7);
    assertThat(response.porTipo())
        .containsExactly(
            new ContagensDeModelosResponse.PorTipo("RESINA", 4),
            new ContagensDeModelosResponse.PorTipo("COQUILHA_ACO", 3));
    assertThat(response.porMaquina())
        .containsExactly(new ContagensDeModelosResponse.PorMaquina("FBOX", 7));
  }

  @Test
  void shouldKeepAnEmptyListWhenNoTipoHasModels() {
    // Act
    final var response =
        ContagensDeModelosResponse.from(new ContagensDeModelos(0, List.of(), List.of()));

    // Assert
    assertThat(response.porTipo()).isEmpty();
    assertThat(response.porMaquina()).isEmpty();
  }

  @Test
  void shouldKeepThePositionAndTheTotalAndAllowANullPosition() {
    // Act
    final var com = PosicaoDoModeloResponse.from(new PosicaoDoModelo(33, 45));
    final var sem = PosicaoDoModeloResponse.from(new PosicaoDoModelo(null, 12));

    // Assert
    assertThat(com).isEqualTo(new PosicaoDoModeloResponse(33, 45));
    assertThat(sem.posicao()).isNull();
    assertThat(sem.total()).isEqualTo(12);
  }
}
