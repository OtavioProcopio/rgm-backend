package com.rgm.api.core.application.usecases.solicitacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RegistrarComentarioUseCaseTest {

  private SolicitacaoRepository solicitacaoRepository;
  private AtividadeSolicitacaoRepository atividadeRepository;
  private RegistrarComentarioUseCase useCase;

  @BeforeEach
  void setUp() {
    solicitacaoRepository = mock(SolicitacaoRepository.class);
    atividadeRepository = mock(AtividadeSolicitacaoRepository.class);
    useCase = new RegistrarComentarioUseCase(solicitacaoRepository, atividadeRepository);
  }

  @Test
  void deveRegistrarComentarioComSucesso() {
    final Instant agora = Instant.now();
    final Solicitacao solicitacao =
        new Solicitacao(
            UUID.randomUUID(),
            "T",
            "D",
            TipoSolicitacao.REPARO,
            StatusSolicitacao.EM_ANDAMENTO,
            PrioridadeSolicitacao.ALTA,
            UUID.randomUUID(),
            UUID.randomUUID(),
            null,
            agora,
            agora,
            null,
            null);

    when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
    when(atividadeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final UUID autorId = UUID.randomUUID();
    final AtividadeSolicitacao resultado =
        useCase.execute(
            new RegistrarComentarioUseCase.Input(solicitacao.getId(), "Comentario", autorId));

    assertNotNull(resultado);
    verify(atividadeRepository).save(any(AtividadeSolicitacao.class));
  }

  @Test
  void deveFalharComComentarioVazio() {
    final Instant agora = Instant.now();
    final Solicitacao solicitacao =
        new Solicitacao(
            UUID.randomUUID(),
            "T",
            "D",
            TipoSolicitacao.REPARO,
            StatusSolicitacao.EM_ANDAMENTO,
            PrioridadeSolicitacao.ALTA,
            UUID.randomUUID(),
            UUID.randomUUID(),
            null,
            agora,
            agora,
            null,
            null);

    when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));

    assertThrows(
        ValidationException.class,
        () ->
            useCase.execute(
                new RegistrarComentarioUseCase.Input(solicitacao.getId(), "", UUID.randomUUID())));
  }

  @Test
  void deveFalharComSolicitacaoNaoEncontrada() {
    final UUID id = UUID.randomUUID();
    when(solicitacaoRepository.findById(id)).thenReturn(Optional.empty());

    assertThrows(
        ValidationException.class,
        () ->
            useCase.execute(
                new RegistrarComentarioUseCase.Input(id, "Comentario", UUID.randomUUID())));
  }
}
