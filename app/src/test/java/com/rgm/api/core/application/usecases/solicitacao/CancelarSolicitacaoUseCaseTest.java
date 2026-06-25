package com.rgm.api.core.application.usecases.solicitacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.events.SolicitacaoFinalizadaEvent;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.DomainEventPublisher;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CancelarSolicitacaoUseCaseTest {

  private SolicitacaoRepository solicitacaoRepository;
  private UsuarioRepository usuarioRepository;
  private AtividadeSolicitacaoRepository atividadeRepository;
  private DomainEventPublisher eventPublisher;
  private CancelarSolicitacaoUseCase useCase;

  @BeforeEach
  void setUp() {
    solicitacaoRepository = mock(SolicitacaoRepository.class);
    usuarioRepository = mock(UsuarioRepository.class);
    atividadeRepository = mock(AtividadeSolicitacaoRepository.class);
    eventPublisher = mock(DomainEventPublisher.class);
    useCase =
        new CancelarSolicitacaoUseCase(
            solicitacaoRepository, usuarioRepository, atividadeRepository, eventPublisher);
  }

  private Solicitacao criarSolicitacao(final StatusSolicitacao status) {
    final Instant agora = Instant.now();
    return new Solicitacao(
        UUID.randomUUID(),
        "Titulo",
        "Descricao",
        TipoSolicitacao.REPARO,
        status,
        status.exigePrioridade() ? PrioridadeSolicitacao.MEDIA : null,
        UUID.randomUUID(),
        UUID.randomUUID(),
        null,
        agora,
        agora,
        null,
        null);
  }

  private Usuario criarUsuario(final PerfilUsuario perfil) {
    final Instant agora = Instant.now();
    return new Usuario(
        UUID.randomUUID(),
        "Nome Usuario",
        "user@test.com",
        "hash",
        perfil,
        true,
        agora,
        agora);
  }

  @Test
  void deveCancelarSolicitacaoComSucesso() {
    final Usuario gestor = criarUsuario(PerfilUsuario.GESTOR);
    final Solicitacao solicitacao = criarSolicitacao(StatusSolicitacao.EM_ANDAMENTO);

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
    when(solicitacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(atividadeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final Solicitacao resultado =
        useCase.execute(
            new CancelarSolicitacaoUseCase.Input(
                solicitacao.getId(), "Solicitacao cancelada por engano", gestor.getId()));

    assertNotNull(resultado);
    assertEquals(StatusSolicitacao.CANCELADA, resultado.getStatus());
    assertNotNull(resultado.getCanceladaEm());
    assertEquals("Solicitacao cancelada por engano", resultado.getComentarioFinal());

    verify(solicitacaoRepository).save(any(Solicitacao.class));
    verify(atividadeRepository, times(2)).save(any());
    verify(eventPublisher).publish(any(SolicitacaoFinalizadaEvent.class));
  }

  @Test
  void deveFalharSeMotivoForEmBranco() {
    final Usuario gestor = criarUsuario(PerfilUsuario.GESTOR);
    final Solicitacao solicitacao = criarSolicitacao(StatusSolicitacao.EM_ANDAMENTO);

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));

    assertThrows(
        ValidationException.class,
        () ->
            useCase.execute(
                new CancelarSolicitacaoUseCase.Input(solicitacao.getId(), "", gestor.getId())));
  }

  @Test
  void deveFalharSeUsuarioNaoForGestorOuAdmin() {
    final Usuario operador = criarUsuario(PerfilUsuario.OPERADOR);
    final Solicitacao solicitacao = criarSolicitacao(StatusSolicitacao.EM_ANDAMENTO);

    when(usuarioRepository.findById(operador.getId())).thenReturn(Optional.of(operador));

    assertThrows(
        NaoAutorizadoException.class,
        () ->
            useCase.execute(
                new CancelarSolicitacaoUseCase.Input(
                    solicitacao.getId(), "Cancelando", operador.getId())));
  }

  @Test
  void deveFalharSeSolicitacaoNaoExistir() {
    final Usuario gestor = criarUsuario(PerfilUsuario.GESTOR);
    final UUID solicitacaoId = UUID.randomUUID();

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(solicitacaoRepository.findById(solicitacaoId)).thenReturn(Optional.empty());

    assertThrows(
        RecursoNaoEncontradoException.class,
        () ->
            useCase.execute(
                new CancelarSolicitacaoUseCase.Input(
                    solicitacaoId, "Cancelando", gestor.getId())));
  }
}
