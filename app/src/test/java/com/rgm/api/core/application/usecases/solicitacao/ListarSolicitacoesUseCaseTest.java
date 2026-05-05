package com.rgm.api.core.application.usecases.solicitacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListarSolicitacoesUseCaseTest {

  private SolicitacaoRepository solicitacaoRepository;
  private ListarSolicitacoesUseCase useCase;

  @BeforeEach
  void setUp() {
    solicitacaoRepository = mock(SolicitacaoRepository.class);
    useCase = new ListarSolicitacoesUseCase(solicitacaoRepository);
  }

  @Test
  void deveListarTodas() {
    final Solicitacao sol =
        Solicitacao.abrir(
            "T", "D", TipoSolicitacao.REPARO, UUID.randomUUID(), UUID.randomUUID(), Instant.now());
    when(solicitacaoRepository.findAll(0, 20))
        .thenReturn(new PageResult<>(List.of(sol), 0, 20, 1, 1));

    final PageResult<Solicitacao> result =
        useCase.execute(new ListarSolicitacoesUseCase.Input(null, null, 0, 20));

    assertEquals(1, result.totalElements());
    verify(solicitacaoRepository).findAll(0, 20);
    verify(solicitacaoRepository, never()).findByStatus(any(), anyInt(), anyInt());
  }

  @Test
  void deveListarPorStatus() {
    when(solicitacaoRepository.findByFilters(StatusSolicitacao.EM_ANDAMENTO, null, 0, 10))
        .thenReturn(new PageResult<>(List.of(), 0, 10, 0, 0));

    final PageResult<Solicitacao> result =
        useCase.execute(
            new ListarSolicitacoesUseCase.Input(StatusSolicitacao.EM_ANDAMENTO, null, 0, 10));

    assertEquals(0, result.totalElements());
    verify(solicitacaoRepository).findByFilters(StatusSolicitacao.EM_ANDAMENTO, null, 0, 10);
    verify(solicitacaoRepository, never()).findAll(anyInt(), anyInt());
  }
}
