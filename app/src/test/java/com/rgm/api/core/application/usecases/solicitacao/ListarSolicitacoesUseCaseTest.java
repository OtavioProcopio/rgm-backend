package com.rgm.api.core.application.usecases.solicitacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoFiltroData;
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
    return input(
        status,
        tipoData,
        dataInicio,
        dataFim,
        abertaPorUsuarioId,
        responsavelId,
        maquina,
        atrasada,
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
      final Boolean emAberto,
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
        emAberto,
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
  void shouldListarTudoWhenNaoHaFiltroNemUsuarioAutenticado() {
    // Arrange
    final PageResult<Solicitacao> esperado = new PageResult<>(List.of(), 0, 20, 0, 0);
    when(solicitacaoRepository.findAll(0, 20)).thenReturn(esperado);

    // Act
    final PageResult<Solicitacao> result =
        useCase.execute(input(null, null, null, null, null, null, null));

    // Assert
    assertSame(esperado, result);
    verify(solicitacaoRepository, times(1)).findAll(0, 20);
    verifyNoMoreInteractions(solicitacaoRepository, usuarioRepository);
  }

  @Test
  void shouldUsarAConsultaDeFiltrosWhenFiltraPorStatus() {
    // Arrange
    final PageResult<Solicitacao> esperado = new PageResult<>(List.of(), 0, 20, 0, 0);
    when(solicitacaoRepository.findByFilters(
            StatusSolicitacao.EM_ANDAMENTO,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            null,
            0,
            20))
        .thenReturn(esperado);

    // Act
    final PageResult<Solicitacao> result =
        useCase.execute(input(StatusSolicitacao.EM_ANDAMENTO, null, null, null, null, null, null));

    // Assert
    assertSame(esperado, result);
    verify(solicitacaoRepository, times(1))
        .findByFilters(
            StatusSolicitacao.EM_ANDAMENTO,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            null,
            0,
            20);
    verifyNoMoreInteractions(solicitacaoRepository, usuarioRepository);
  }

  @Test
  void shouldUsarAConsultaDeFiltrosWhenFiltraPorMaquina() {
    // Arrange
    final PageResult<Solicitacao> esperado = new PageResult<>(List.of(), 0, 20, 0, 0);
    when(solicitacaoRepository.findByFilters(
            null, null, null, null, null, null, null, null, null, null, "VICK", null, false, null,
            0, 20))
        .thenReturn(esperado);

    // Act
    final PageResult<Solicitacao> result =
        useCase.execute(input(null, null, null, null, null, null, "VICK", null));

    // Assert
    assertSame(esperado, result);
    verify(solicitacaoRepository, times(1))
        .findByFilters(
            null, null, null, null, null, null, null, null, null, null, "VICK", null, false, null,
            0, 20);
    verifyNoMoreInteractions(solicitacaoRepository, usuarioRepository);
  }

  @Test
  void shouldUsarAConsultaDeFiltrosWhenFiltraPorAtrasada() {
    // Arrange
    final PageResult<Solicitacao> esperado = new PageResult<>(List.of(), 0, 20, 0, 0);
    when(solicitacaoRepository.findByFilters(
            null, null, null, null, null, null, null, null, null, null, null, true, false, null, 0,
            20))
        .thenReturn(esperado);

    // Act
    final PageResult<Solicitacao> result =
        useCase.execute(input(null, null, null, null, null, null, null, true, null));

    // Assert
    assertSame(esperado, result);
    verify(solicitacaoRepository, times(1))
        .findByFilters(
            null, null, null, null, null, null, null, null, null, null, null, true, false, null, 0,
            20);
    verifyNoMoreInteractions(solicitacaoRepository, usuarioRepository);
  }

  @Test
  void shouldRestringirAoQueOOperadorVeWhenOperadorListaSemFiltro() {
    // Arrange
    final UUID operadorId = UUID.randomUUID();
    final Usuario operador = usuarioComPerfil(operadorId, PerfilUsuario.OPERADOR);
    final PageResult<Solicitacao> esperado = new PageResult<>(List.of(), 0, 20, 0, 0);
    when(solicitacaoRepository.findByFilters(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            operadorId,
            0,
            20))
        .thenReturn(esperado);

    // Act
    final PageResult<Solicitacao> result =
        useCase.execute(input(null, null, null, null, null, null, operadorId));

    // Assert
    assertSame(esperado, result);
    verify(usuarioRepository, times(1)).findById(operadorId);
    verify(operador, times(1)).getPerfil();
    verify(solicitacaoRepository, times(1))
        .findByFilters(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            operadorId,
            0,
            20);
    verifyNoMoreInteractions(solicitacaoRepository, usuarioRepository, operador);
  }

  @Test
  void shouldRepassarOsFiltrosDoOperadorSemTrocarOResponsavelWhenOperadorFiltra() {
    // Arrange
    final UUID operadorId = UUID.randomUUID();
    final UUID abertaPor = UUID.randomUUID();
    final UUID responsavel = UUID.randomUUID();
    final Usuario operador = usuarioComPerfil(operadorId, PerfilUsuario.OPERADOR);
    final PageResult<Solicitacao> esperado = new PageResult<>(List.of(), 0, 20, 0, 0);
    when(solicitacaoRepository.findByFilters(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            abertaPor,
            responsavel,
            null,
            null,
            false,
            operadorId,
            0,
            20))
        .thenReturn(esperado);

    // Act
    final PageResult<Solicitacao> result =
        useCase.execute(input(null, null, null, null, abertaPor, responsavel, operadorId));

    // Assert
    assertSame(esperado, result);
    verify(usuarioRepository, times(1)).findById(operadorId);
    verify(operador, times(1)).getPerfil();
    verify(solicitacaoRepository, times(1))
        .findByFilters(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            abertaPor,
            responsavel,
            null,
            null,
            false,
            operadorId,
            0,
            20);
    verifyNoMoreInteractions(solicitacaoRepository, usuarioRepository, operador);
  }

  @Test
  void shouldListarTudoWhenGestorListaSemFiltro() {
    // Arrange
    final UUID gestorId = UUID.randomUUID();
    final Usuario gestor = usuarioComPerfil(gestorId, PerfilUsuario.GESTOR);
    final PageResult<Solicitacao> esperado = new PageResult<>(List.of(), 0, 20, 0, 0);
    when(solicitacaoRepository.findAll(0, 20)).thenReturn(esperado);

    // Act
    final PageResult<Solicitacao> result =
        useCase.execute(input(null, null, null, null, null, null, gestorId));

    // Assert
    assertSame(esperado, result);
    verify(usuarioRepository, times(1)).findById(gestorId);
    verify(gestor, times(1)).getPerfil();
    verify(solicitacaoRepository, times(1)).findAll(0, 20);
    verifyNoMoreInteractions(solicitacaoRepository, usuarioRepository, gestor);
  }

  @Test
  void shouldNaoRestringirAVisibilidadeWhenAdministradorFiltra() {
    // Arrange
    final UUID adminId = UUID.randomUUID();
    final UUID abertaPor = UUID.randomUUID();
    final Usuario admin = usuarioComPerfil(adminId, PerfilUsuario.ADMINISTRADOR);
    final PageResult<Solicitacao> esperado = new PageResult<>(List.of(), 0, 20, 0, 0);
    when(solicitacaoRepository.findByFilters(
            null, null, null, null, null, null, null, null, abertaPor, null, null, null, false,
            null, 0, 20))
        .thenReturn(esperado);

    // Act
    final PageResult<Solicitacao> result =
        useCase.execute(input(null, null, null, null, abertaPor, null, adminId));

    // Assert
    assertSame(esperado, result);
    verify(usuarioRepository, times(1)).findById(adminId);
    verify(admin, times(1)).getPerfil();
    verify(solicitacaoRepository, times(1))
        .findByFilters(
            null, null, null, null, null, null, null, null, abertaPor, null, null, null, false,
            null, 0, 20);
    verifyNoMoreInteractions(solicitacaoRepository, usuarioRepository, admin);
  }

  @Test
  void shouldManterOResponsavelPedidoWhenGestorFiltraPorResponsavel() {
    // Arrange
    final UUID gestorId = UUID.randomUUID();
    final UUID responsavel = UUID.randomUUID();
    final Usuario gestor = usuarioComPerfil(gestorId, PerfilUsuario.GESTOR);
    final PageResult<Solicitacao> esperado = new PageResult<>(List.of(), 0, 20, 0, 0);
    when(solicitacaoRepository.findByFilters(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            responsavel,
            null,
            null,
            false,
            null,
            0,
            20))
        .thenReturn(esperado);

    // Act
    final PageResult<Solicitacao> result =
        useCase.execute(input(null, null, null, null, null, responsavel, gestorId));

    // Assert
    assertSame(esperado, result);
    verify(usuarioRepository, times(1)).findById(gestorId);
    verify(gestor, times(1)).getPerfil();
    verify(solicitacaoRepository, times(1))
        .findByFilters(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            responsavel,
            null,
            null,
            false,
            null,
            0,
            20);
    verifyNoMoreInteractions(solicitacaoRepository, usuarioRepository, gestor);
  }

  @Test
  void shouldCompararADataDeEncerramentoWhenFiltroEPorConclusao() {
    // Arrange
    final Instant inicio = Instant.parse("2026-01-01T00:00:00Z");
    final Instant fim = Instant.parse("2026-02-01T00:00:00Z");
    final PageResult<Solicitacao> esperado = new PageResult<>(List.of(), 0, 20, 0, 0);
    when(solicitacaoRepository.findByFilters(
            null, null, null, null, null, null, inicio, fim, null, null, null, null, false, null, 0,
            20))
        .thenReturn(esperado);

    // Act
    final PageResult<Solicitacao> result =
        useCase.execute(input(null, TipoFiltroData.CONCLUSAO, inicio, fim, null, null, null));

    // Assert
    assertSame(esperado, result);
    verify(solicitacaoRepository, times(1))
        .findByFilters(
            null, null, null, null, null, null, inicio, fim, null, null, null, null, false, null, 0,
            20);
    verifyNoMoreInteractions(solicitacaoRepository, usuarioRepository);
  }

  @Test
  void shouldCompararADataDeCriacaoWhenFiltroEPorCriacao() {
    // Arrange
    final Instant inicio = Instant.parse("2026-01-01T00:00:00Z");
    final Instant fim = Instant.parse("2026-02-01T00:00:00Z");
    final PageResult<Solicitacao> esperado = new PageResult<>(List.of(), 0, 20, 0, 0);
    when(solicitacaoRepository.findByFilters(
            null, null, null, null, inicio, fim, null, null, null, null, null, null, false, null, 0,
            20))
        .thenReturn(esperado);

    // Act
    final PageResult<Solicitacao> result =
        useCase.execute(input(null, TipoFiltroData.CRIACAO, inicio, fim, null, null, null));

    // Assert
    assertSame(esperado, result);
    verify(solicitacaoRepository, times(1))
        .findByFilters(
            null, null, null, null, inicio, fim, null, null, null, null, null, null, false, null, 0,
            20);
    verifyNoMoreInteractions(solicitacaoRepository, usuarioRepository);
  }

  @Test
  void shouldRepassarEmAbertoAoRepositorioWhenFiltroVemLigadoComOutroFiltro() {
    // Arrange
    final UUID abertaPor = UUID.randomUUID();
    final PageResult<Solicitacao> esperado = new PageResult<>(List.of(), 0, 20, 0, 0);
    when(solicitacaoRepository.findByFilters(
            null, null, null, null, null, null, null, null, abertaPor, null, null, null, true, null,
            0, 20))
        .thenReturn(esperado);

    // Act
    final PageResult<Solicitacao> result =
        useCase.execute(input(null, null, null, null, abertaPor, null, null, null, true, null));

    // Assert
    assertSame(esperado, result);
    verify(solicitacaoRepository, times(1))
        .findByFilters(
            null, null, null, null, null, null, null, null, abertaPor, null, null, null, true, null,
            0, 20);
    verifyNoMoreInteractions(solicitacaoRepository);
  }

  @Test
  void shouldUsarAConsultaDeFiltrosWhenEmAbertoEOUnicoFiltro() {
    // Arrange
    final PageResult<Solicitacao> esperado = new PageResult<>(List.of(), 0, 20, 0, 0);
    when(solicitacaoRepository.findByFilters(
            null, null, null, null, null, null, null, null, null, null, null, null, true, null, 0,
            20))
        .thenReturn(esperado);

    // Act
    final PageResult<Solicitacao> result =
        useCase.execute(input(null, null, null, null, null, null, null, null, true, null));

    // Assert
    assertSame(esperado, result);
    verify(solicitacaoRepository, times(1))
        .findByFilters(
            null, null, null, null, null, null, null, null, null, null, null, null, true, null, 0,
            20);
    verifyNoMoreInteractions(solicitacaoRepository);
  }

  @Test
  void shouldListarTudoWhenEmAbertoVemDesligadoSemOutroFiltro() {
    // Arrange
    final PageResult<Solicitacao> esperado = new PageResult<>(List.of(), 0, 20, 0, 0);
    when(solicitacaoRepository.findAll(0, 20)).thenReturn(esperado);

    // Act
    final PageResult<Solicitacao> result =
        useCase.execute(input(null, null, null, null, null, null, null, null, false, null));

    // Assert
    assertSame(esperado, result);
    verify(solicitacaoRepository, times(1)).findAll(0, 20);
    verifyNoMoreInteractions(solicitacaoRepository);
  }
}
