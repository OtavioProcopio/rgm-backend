package com.rgm.api.core.application.usecases.evidencia;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Evidencia;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.entities.SolicitacaoEvidencia;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
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
            UUID.randomUUID(),
            null,
            agora,
            agora,
            null,
            null);

    final UUID evId1 = UUID.randomUUID();
    final UUID evId2 = UUID.randomUUID();
    final Evidencia ev1 =
        new Evidencia(evId1, "http://url1", "image/png", "f1.png", 100, UUID.randomUUID(), agora);
    final Evidencia ev2 =
        new Evidencia(evId2, "http://url2", "image/jpeg", "f2.jpg", 200, UUID.randomUUID(), agora);

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
}
