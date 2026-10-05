package com.rgm.api.core.application.usecases.evidencia;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Evidencia;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.entities.SolicitacaoEvidencia;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoEvidencia;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.EvidenciaRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoEvidenciaRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VisualizarEvidenciaUseCaseTest {

  private SolicitacaoRepository solicitacaoRepository;
  private SolicitacaoEvidenciaRepository solicitacaoEvidenciaRepository;
  private EvidenciaRepository evidenciaRepository;
  private UsuarioRepository usuarioRepository;
  private SolicitacaoAtribuicaoRepository atribuicaoRepository;
  private VisualizarEvidenciaUseCase useCase;

  @BeforeEach
  void setUp() {
    solicitacaoRepository = mock(SolicitacaoRepository.class);
    solicitacaoEvidenciaRepository = mock(SolicitacaoEvidenciaRepository.class);
    evidenciaRepository = mock(EvidenciaRepository.class);
    usuarioRepository = mock(UsuarioRepository.class);
    atribuicaoRepository = mock(SolicitacaoAtribuicaoRepository.class);
    useCase =
        new VisualizarEvidenciaUseCase(
            solicitacaoRepository,
            solicitacaoEvidenciaRepository,
            evidenciaRepository,
            usuarioRepository,
            atribuicaoRepository);
  }

  @Test
  void deveRetornarEvidenciasDaSolicitacao() {
    final Instant agora = Instant.now();
    final UUID solId = UUID.randomUUID();
    final UUID usuarioId = UUID.randomUUID();
    final Solicitacao sol =
        new Solicitacao(
            solId,
            "T",
            "D",
            TipoSolicitacao.REPARO,
            StatusSolicitacao.EM_ANDAMENTO,
            PrioridadeSolicitacao.ALTA,
            UUID.randomUUID(),
            null /* modeloCodigo */,
            null /* modeloMaquina */,
            null /* modeloObservacoes */,
            UUID.randomUUID(),
            null,
            agora,
            agora,
            null,
            null);

    final UUID evId1 = UUID.randomUUID();
    final UUID evId2 = UUID.randomUUID();
    final Evidencia ev1 =
        new Evidencia(
            evId1,
            "http://url1",
            "image/png",
            "f1.png",
            100,
            UUID.randomUUID(),
            agora,
            TipoEvidencia.GERAL,
            null);
    final Evidencia ev2 =
        new Evidencia(
            evId2,
            "http://url2",
            "image/jpeg",
            "f2.jpg",
            200,
            UUID.randomUUID(),
            agora,
            TipoEvidencia.GERAL,
            null);

    when(solicitacaoRepository.findById(solId)).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(usuarioId))
        .thenReturn(
            Optional.of(
                Usuario.criarInterno("Test", "t@t.com", "hash", PerfilUsuario.GESTOR, agora)));
    when(solicitacaoEvidenciaRepository.findBySolicitacaoId(solId))
        .thenReturn(
            List.of(
                new SolicitacaoEvidencia(solId, evId1), new SolicitacaoEvidencia(solId, evId2)));
    when(evidenciaRepository.findById(evId1)).thenReturn(Optional.of(ev1));
    when(evidenciaRepository.findById(evId2)).thenReturn(Optional.of(ev2));

    final List<Evidencia> resultado =
        useCase.execute(new VisualizarEvidenciaUseCase.Input(solId, usuarioId));

    assertEquals(2, resultado.size());
    assertEquals("http://url1", resultado.get(0).getPublicUrl());
    assertEquals("http://url2", resultado.get(1).getPublicUrl());
  }

  @Test
  void deveRetornarListaVaziaSeNaoHaEvidencias() {
    final Instant agora = Instant.now();
    final UUID solId = UUID.randomUUID();
    final UUID usuarioId = UUID.randomUUID();
    final Solicitacao sol =
        new Solicitacao(
            solId,
            "T",
            "D",
            TipoSolicitacao.REPARO,
            StatusSolicitacao.A_FAZER,
            null,
            UUID.randomUUID(),
            null /* modeloCodigo */,
            null /* modeloMaquina */,
            null /* modeloObservacoes */,
            UUID.randomUUID(),
            null,
            agora,
            agora,
            null,
            null);

    when(solicitacaoRepository.findById(solId)).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(usuarioId))
        .thenReturn(
            Optional.of(
                Usuario.criarInterno(
                    "Test", "t@t.com", "hash", PerfilUsuario.ADMINISTRADOR, agora)));
    when(solicitacaoEvidenciaRepository.findBySolicitacaoId(solId)).thenReturn(List.of());

    final List<Evidencia> resultado =
        useCase.execute(new VisualizarEvidenciaUseCase.Input(solId, usuarioId));

    assertTrue(resultado.isEmpty());
  }

  @Test
  void deveFalharComSolicitacaoNaoEncontrada() {
    when(solicitacaoRepository.findById(any())).thenReturn(Optional.empty());

    assertThrows(
        RecursoNaoEncontradoException.class,
        () ->
            useCase.execute(
                new VisualizarEvidenciaUseCase.Input(UUID.randomUUID(), UUID.randomUUID())));
  }

  private Usuario usuario(final UUID id, final PerfilUsuario perfil, final boolean ativo) {
    final Instant agora = Instant.now();
    return new Usuario(id, "Usuario", "usuario@rgm.test", "hash", perfil, ativo, agora, agora);
  }

  private Solicitacao solicitacaoAbertaPor(final UUID autorId, final StatusSolicitacao status) {
    final Instant agora = Instant.now();
    return new Solicitacao(
        UUID.randomUUID(),
        "T",
        "D",
        TipoSolicitacao.REPARO,
        status,
        status.exigePrioridade() ? PrioridadeSolicitacao.ALTA : null,
        UUID.randomUUID(),
        null,
        null,
        null,
        autorId,
        status.isTerminal() ? "Comentario final" : null,
        agora,
        agora,
        status == StatusSolicitacao.CONCLUIDA ? agora : null,
        status == StatusSolicitacao.CANCELADA ? agora : null);
  }

  private Evidencia evidencia(final UUID id) {
    return new Evidencia(
        id,
        "http://storage/evidencias/foto.jpg",
        "image/jpeg",
        "foto.jpg",
        1024,
        UUID.randomUUID(),
        Instant.now(),
        TipoEvidencia.GERAL,
        null);
  }

  private void verificarQueNadaMaisFoiChamado() {
    verifyNoMoreInteractions(
        solicitacaoRepository,
        solicitacaoEvidenciaRepository,
        evidenciaRepository,
        usuarioRepository,
        atribuicaoRepository);
  }

  @Test
  void shouldListEvidenceWhenUserOpenedTheSolicitacao() {
    // Arrange
    final UUID autorId = UUID.randomUUID();
    final UUID evidenciaId = UUID.randomUUID();
    final Solicitacao sol = solicitacaoAbertaPor(autorId, StatusSolicitacao.A_FAZER);
    final Evidencia evidencia = evidencia(evidenciaId);
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(autorId))
        .thenReturn(Optional.of(usuario(autorId, PerfilUsuario.OPERADOR, true)));
    when(atribuicaoRepository.existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(
            sol.getId(), autorId))
        .thenReturn(false);
    when(solicitacaoEvidenciaRepository.findBySolicitacaoId(sol.getId()))
        .thenReturn(List.of(new SolicitacaoEvidencia(sol.getId(), evidenciaId)));
    when(evidenciaRepository.findById(evidenciaId)).thenReturn(Optional.of(evidencia));

    // Act
    final List<Evidencia> resultado =
        useCase.execute(new VisualizarEvidenciaUseCase.Input(sol.getId(), autorId));

    // Assert
    assertEquals(List.of(evidencia), resultado);
    verify(solicitacaoRepository, times(1)).findById(sol.getId());
    verify(usuarioRepository, times(1)).findById(autorId);
    verify(atribuicaoRepository, times(1))
        .existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(sol.getId(), autorId);
    verify(solicitacaoEvidenciaRepository, times(1)).findBySolicitacaoId(sol.getId());
    verify(evidenciaRepository, times(1)).findById(evidenciaId);
    verificarQueNadaMaisFoiChamado();
  }

  @Test
  void shouldListEvidenceWhenAuthorReadsClosedSolicitacao() {
    // Arrange
    final UUID autorId = UUID.randomUUID();
    final UUID evidenciaId = UUID.randomUUID();
    final Solicitacao sol = solicitacaoAbertaPor(autorId, StatusSolicitacao.CANCELADA);
    final Evidencia evidencia = evidencia(evidenciaId);
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(autorId))
        .thenReturn(Optional.of(usuario(autorId, PerfilUsuario.OPERADOR, true)));
    when(atribuicaoRepository.existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(
            sol.getId(), autorId))
        .thenReturn(false);
    when(solicitacaoEvidenciaRepository.findBySolicitacaoId(sol.getId()))
        .thenReturn(List.of(new SolicitacaoEvidencia(sol.getId(), evidenciaId)));
    when(evidenciaRepository.findById(evidenciaId)).thenReturn(Optional.of(evidencia));

    // Act
    final List<Evidencia> resultado =
        useCase.execute(new VisualizarEvidenciaUseCase.Input(sol.getId(), autorId));

    // Assert
    assertEquals(List.of(evidencia), resultado);
    verify(solicitacaoRepository, times(1)).findById(sol.getId());
    verify(usuarioRepository, times(1)).findById(autorId);
    verify(atribuicaoRepository, times(1))
        .existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(sol.getId(), autorId);
    verify(solicitacaoEvidenciaRepository, times(1)).findBySolicitacaoId(sol.getId());
    verify(evidenciaRepository, times(1)).findById(evidenciaId);
    verificarQueNadaMaisFoiChamado();
  }

  @Test
  void shouldDenyListingWhenOperadorHasNoRelationWithTheSolicitacao() {
    // Arrange
    final UUID estranhoId = UUID.randomUUID();
    final Solicitacao sol = solicitacaoAbertaPor(UUID.randomUUID(), StatusSolicitacao.A_FAZER);
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(estranhoId))
        .thenReturn(Optional.of(usuario(estranhoId, PerfilUsuario.OPERADOR, true)));
    when(atribuicaoRepository.existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(
            sol.getId(), estranhoId))
        .thenReturn(false);

    // Act
    final NaoAutorizadoException erro =
        assertThrows(
            NaoAutorizadoException.class,
            () -> useCase.execute(new VisualizarEvidenciaUseCase.Input(sol.getId(), estranhoId)));

    // Assert
    assertEquals("Usuario nao tem acesso a esta solicitacao", erro.getMessage());
    verify(solicitacaoRepository, times(1)).findById(sol.getId());
    verify(usuarioRepository, times(1)).findById(estranhoId);
    verify(atribuicaoRepository, times(1))
        .existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(sol.getId(), estranhoId);
    verificarQueNadaMaisFoiChamado();
  }

  @Test
  void shouldListEvidenceWithoutCheckingAssignmentWhenUserIsGestor() {
    // Arrange
    final UUID gestorId = UUID.randomUUID();
    final Solicitacao sol = solicitacaoAbertaPor(UUID.randomUUID(), StatusSolicitacao.A_FAZER);
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(gestorId))
        .thenReturn(Optional.of(usuario(gestorId, PerfilUsuario.GESTOR, true)));
    when(solicitacaoEvidenciaRepository.findBySolicitacaoId(sol.getId())).thenReturn(List.of());

    // Act
    final List<Evidencia> resultado =
        useCase.execute(new VisualizarEvidenciaUseCase.Input(sol.getId(), gestorId));

    // Assert
    assertEquals(List.of(), resultado);
    verify(solicitacaoRepository, times(1)).findById(sol.getId());
    verify(usuarioRepository, times(1)).findById(gestorId);
    verify(solicitacaoEvidenciaRepository, times(1)).findBySolicitacaoId(sol.getId());
    verificarQueNadaMaisFoiChamado();
  }

  @Test
  void shouldDenyListingWhenUserIsInactive() {
    // Arrange
    final UUID autorId = UUID.randomUUID();
    final Solicitacao sol = solicitacaoAbertaPor(autorId, StatusSolicitacao.A_FAZER);
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(autorId))
        .thenReturn(Optional.of(usuario(autorId, PerfilUsuario.OPERADOR, false)));

    // Act
    final NaoAutorizadoException erro =
        assertThrows(
            NaoAutorizadoException.class,
            () -> useCase.execute(new VisualizarEvidenciaUseCase.Input(sol.getId(), autorId)));

    // Assert
    assertEquals("Usuario inativo", erro.getMessage());
    verify(solicitacaoRepository, times(1)).findById(sol.getId());
    verify(usuarioRepository, times(1)).findById(autorId);
    verificarQueNadaMaisFoiChamado();
  }

  @Test
  void shouldFailWhenUserDoesNotExist() {
    // Arrange
    final UUID usuarioId = UUID.randomUUID();
    final Solicitacao sol = solicitacaoAbertaPor(usuarioId, StatusSolicitacao.A_FAZER);
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

    // Act
    final RecursoNaoEncontradoException erro =
        assertThrows(
            RecursoNaoEncontradoException.class,
            () -> useCase.execute(new VisualizarEvidenciaUseCase.Input(sol.getId(), usuarioId)));

    // Assert
    assertEquals("Usuario nao encontrado", erro.getMessage());
    verify(solicitacaoRepository, times(1)).findById(sol.getId());
    verify(usuarioRepository, times(1)).findById(usuarioId);
    verificarQueNadaMaisFoiChamado();
  }
}
