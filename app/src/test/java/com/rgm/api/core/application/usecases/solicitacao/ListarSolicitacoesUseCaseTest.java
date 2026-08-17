package com.rgm.api.core.application.usecases.solicitacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoFiltroData;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListarSolicitacoesUseCaseTest {

  private SolicitacaoRepository solicitacaoRepository;
  private UsuarioRepository usuarioRepository;
  private ListarSolicitacoesUseCase useCase;

  @BeforeEach
  void setUp() {
    solicitacaoRepository = mock(SolicitacaoRepository.class);
    usuarioRepository = mock(UsuarioRepository.class);
    useCase = new ListarSolicitacoesUseCase(solicitacaoRepository, usuarioRepository);
  }

  private ListarSolicitacoesUseCase.Input input(
      final StatusSolicitacao status,
      final TipoFiltroData tipoData,
      final Instant dataInicio,
      final Instant dataFim,
      final UUID abertaPorUsuarioId,
      final UUID responsavelId,
      final UUID usuarioAutenticadoId) {
    return input(
        status,
        tipoData,
        dataInicio,
        dataFim,
        abertaPorUsuarioId,
        responsavelId,
        null,
        usuarioAutenticadoId);
  }

  private ListarSolicitacoesUseCase.Input input(
      final StatusSolicitacao status,
      final TipoFiltroData tipoData,
      final Instant dataInicio,
      final Instant dataFim,
      final UUID abertaPorUsuarioId,
      final UUID responsavelId,
      final String maquina,
      final UUID usuarioAutenticadoId) {
    return input(
        status,
        tipoData,
        dataInicio,
        dataFim,
        abertaPorUsuarioId,
        responsavelId,
        maquina,
        null,
        usuarioAutenticadoId);
  }

  private ListarSolicitacoesUseCase.Input input(
      final StatusSolicitacao status,
      final TipoFiltroData tipoData,
      final Instant dataInicio,
      final Instant dataFim,
      final UUID abertaPorUsuarioId,
      final UUID responsavelId,
      final String maquina,
      final Boolean atrasada,
      final UUID usuarioAutenticadoId) {
    return new ListarSolicitacoesUseCase.Input(
        status,
        null,
        null,
        null,
        tipoData,
        dataInicio,
        dataFim,
        abertaPorUsuarioId,
        responsavelId,
        maquina,
        atrasada,
        usuarioAutenticadoId,
        0,
        20);
  }

  private Usuario usuarioComPerfil(final UUID id, final PerfilUsuario perfil) {
    final Usuario usuario = mock(Usuario.class);
    when(usuario.getPerfil()).thenReturn(perfil);
    when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));
    return usuario;
  }

  @Test
  void deveListarTodas() {
    final Solicitacao sol =
        Solicitacao.abrir(
            "T", "D", TipoSolicitacao.REPARO, UUID.randomUUID(), UUID.randomUUID(), Instant.now());
    when(solicitacaoRepository.findAll(0, 20))
        .thenReturn(new PageResult<>(List.of(sol), 0, 20, 1, 1));

    final PageResult<Solicitacao> result =
        useCase.execute(input(null, null, null, null, null, null, null));

    assertEquals(1, result.totalElements());
    verify(solicitacaoRepository).findAll(0, 20);
  }

  @Test
  void deveListarPorStatus() {
    when(solicitacaoRepository.findByFilters(
            eq(StatusSolicitacao.EM_ANDAMENTO),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            eq(0),
            eq(20)))
        .thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));

    final PageResult<Solicitacao> result =
        useCase.execute(input(StatusSolicitacao.EM_ANDAMENTO, null, null, null, null, null, null));

    assertEquals(0, result.totalElements());
    verify(solicitacaoRepository, never()).findAll(anyInt(), anyInt());
  }

  @Test
  void deveListarPorMaquina() {
    when(solicitacaoRepository.findByFilters(
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            eq("VICK"),
            isNull(),
            eq(0),
            eq(20)))
        .thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));

    final PageResult<Solicitacao> result =
        useCase.execute(input(null, null, null, null, null, null, "VICK", null));

    assertEquals(0, result.totalElements());
    verify(solicitacaoRepository, never()).findAll(anyInt(), anyInt());
  }

  @Test
  void deveListarPorAtrasada() {
    when(solicitacaoRepository.findByFilters(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
            isNull(), isNull(), isNull(), eq(true), eq(0), eq(20)))
        .thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));

    final PageResult<Solicitacao> result =
        useCase.execute(input(null, null, null, null, null, null, null, true, null));

    assertEquals(0, result.totalElements());
    verify(solicitacaoRepository, never()).findAll(anyInt(), anyInt());
  }

  @Test
  void operadorSoVeAsSuas() {
    final UUID operadorId = UUID.randomUUID();
    final UUID outroResponsavel = UUID.randomUUID();
    usuarioComPerfil(operadorId, PerfilUsuario.OPERADOR);
    when(solicitacaoRepository.findByFilters(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            eq(operadorId),
            any(),
            any(),
            anyInt(),
            anyInt()))
        .thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));

    useCase.execute(input(null, null, null, null, null, outroResponsavel, operadorId));

    verify(solicitacaoRepository)
        .findByFilters(
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            eq(operadorId),
            isNull(),
            isNull(),
            eq(0),
            eq(20));
  }

  @Test
  void gestorMantemResponsavelDoClient() {
    final UUID gestorId = UUID.randomUUID();
    final UUID responsavelFiltrado = UUID.randomUUID();
    usuarioComPerfil(gestorId, PerfilUsuario.GESTOR);
    when(solicitacaoRepository.findByFilters(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            eq(responsavelFiltrado),
            any(),
            any(),
            anyInt(),
            anyInt()))
        .thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));

    useCase.execute(input(null, null, null, null, null, responsavelFiltrado, gestorId));

    verify(solicitacaoRepository)
        .findByFilters(
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            eq(responsavelFiltrado),
            isNull(),
            isNull(),
            eq(0),
            eq(20));
  }

  @Test
  void filtroPorConclusaoRoteiaParaConcluidaEm() {
    final Instant inicio = Instant.parse("2026-01-01T00:00:00Z");
    final Instant fim = Instant.parse("2026-02-01T00:00:00Z");
    when(solicitacaoRepository.findByFilters(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            anyInt(), anyInt()))
        .thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));

    useCase.execute(input(null, TipoFiltroData.CONCLUSAO, inicio, fim, null, null, null));

    verify(solicitacaoRepository)
        .findByFilters(
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            eq(inicio),
            eq(fim),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            eq(0),
            eq(20));
  }

  @Test
  void filtroPorCriacaoRoteiaParaCriadaEm() {
    final Instant inicio = Instant.parse("2026-01-01T00:00:00Z");
    final Instant fim = Instant.parse("2026-02-01T00:00:00Z");
    when(solicitacaoRepository.findByFilters(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            anyInt(), anyInt()))
        .thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));

    useCase.execute(input(null, TipoFiltroData.CRIACAO, inicio, fim, null, null, null));

    verify(solicitacaoRepository)
        .findByFilters(
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            eq(inicio),
            eq(fim),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            eq(0),
            eq(20));
  }
}
