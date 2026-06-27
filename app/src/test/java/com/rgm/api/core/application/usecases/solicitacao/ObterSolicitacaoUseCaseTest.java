package com.rgm.api.core.application.usecases.solicitacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.entities.SolicitacaoAtribuicao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ObterSolicitacaoUseCaseTest {

  @Mock private SolicitacaoRepository solicitacaoRepository;
  @Mock private SolicitacaoAtribuicaoRepository atribuicaoRepository;
  @InjectMocks private ObterSolicitacaoUseCase useCase;

  @Test
  void execute_retornaSolicitacaoComResponsaveis() {
    final UUID solId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final Solicitacao sol =
        Solicitacao.abrir("T", "D", TipoSolicitacao.REPARO, UUID.randomUUID(), userId, Instant.now());

    final SolicitacaoAtribuicao atrib =
        new SolicitacaoAtribuicao(UUID.randomUUID(), solId, userId, UUID.randomUUID(), Instant.now(), null);

    when(solicitacaoRepository.findById(solId)).thenReturn(Optional.of(sol));
    when(atribuicaoRepository.findBySolicitacaoId(solId)).thenReturn(List.of(atrib));

    final var output = useCase.execute(solId);

    assertThat(output.solicitacao()).isEqualTo(sol);
    assertThat(output.responsavelIds()).containsExactly(userId);
  }

  @Test
  void execute_filtraRemovidoEm() {
    final UUID solId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final Solicitacao sol =
        Solicitacao.abrir("T", "D", TipoSolicitacao.REPARO, UUID.randomUUID(), userId, Instant.now());

    final SolicitacaoAtribuicao removida =
        new SolicitacaoAtribuicao(UUID.randomUUID(), solId, userId, UUID.randomUUID(), Instant.now(), Instant.now());

    when(solicitacaoRepository.findById(solId)).thenReturn(Optional.of(sol));
    when(atribuicaoRepository.findBySolicitacaoId(solId)).thenReturn(List.of(removida));

    final var output = useCase.execute(solId);

    assertThat(output.responsavelIds()).isEmpty();
  }

  @Test
  void execute_lancaExcecaoSeNaoEncontrado() {
    final UUID solId = UUID.randomUUID();
    when(solicitacaoRepository.findById(solId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.execute(solId))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }

  @Test
  void listarResponsaveisBatch_vazio() {
    final var result = useCase.listarResponsaveisBatch(List.of());
    assertThat(result).isEmpty();
  }

  @Test
  void listarResponsaveisBatch_agrupaPorSolicitacao() {
    final UUID solId1 = UUID.randomUUID();
    final UUID solId2 = UUID.randomUUID();
    final UUID user1 = UUID.randomUUID();
    final UUID user2 = UUID.randomUUID();

    final var atrib1 = new SolicitacaoAtribuicao(UUID.randomUUID(), solId1, user1, UUID.randomUUID(), Instant.now(), null);
    final var atrib2 = new SolicitacaoAtribuicao(UUID.randomUUID(), solId2, user2, UUID.randomUUID(), Instant.now(), null);
    final var removida = new SolicitacaoAtribuicao(UUID.randomUUID(), solId1, user2, UUID.randomUUID(), Instant.now(), Instant.now());

    when(atribuicaoRepository.findBySolicitacaoIdIn(List.of(solId1, solId2)))
        .thenReturn(List.of(atrib1, atrib2, removida));

    final var result = useCase.listarResponsaveisBatch(List.of(solId1, solId2));

    assertThat(result.get(solId1)).containsExactly(user1);
    assertThat(result.get(solId2)).containsExactly(user2);
  }
}
