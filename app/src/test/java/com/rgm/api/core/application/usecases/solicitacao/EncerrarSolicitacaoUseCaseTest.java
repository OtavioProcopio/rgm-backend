package com.rgm.api.core.application.usecases.solicitacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.events.SolicitacaoFinalizadaEvent;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.model.aggregates.EventoModelo;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.EventoModeloRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.DomainEventPublisher;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EncerrarSolicitacaoUseCaseTest {

  private SolicitacaoRepository solicitacaoRepository;
  private UsuarioRepository usuarioRepository;
  private AtividadeSolicitacaoRepository atividadeRepository;
  private EventoModeloRepository eventoModeloRepository;
  private DomainEventPublisher eventPublisher;
  private EncerrarSolicitacaoUseCase useCase;

  @BeforeEach
  void setUp() {
    solicitacaoRepository = mock(SolicitacaoRepository.class);
    usuarioRepository = mock(UsuarioRepository.class);
    atividadeRepository = mock(AtividadeSolicitacaoRepository.class);
    eventoModeloRepository = mock(EventoModeloRepository.class);
    eventPublisher = mock(DomainEventPublisher.class);
    useCase =
        new EncerrarSolicitacaoUseCase(
            solicitacaoRepository,
            usuarioRepository,
            atividadeRepository,
            eventoModeloRepository,
            eventPublisher);
  }

  private Solicitacao criarSolicitacaoEmValidacao() {
    final Instant agora = Instant.now();
    return new Solicitacao(
        UUID.randomUUID(),
        "Titulo",
        "Descricao",
        TipoSolicitacao.REPARO,
        StatusSolicitacao.EM_VALIDACAO,
        PrioridadeSolicitacao.ALTA,
        UUID.randomUUID(),
        UUID.randomUUID(),
        null,
        agora,
        agora,
        null,
        null);
  }

  private Usuario criarGestor() {
    final Instant agora = Instant.now();
    return new Usuario(
        UUID.randomUUID(),
        "Gestor",
        "gestor@test.com",
        "hash",
        PerfilUsuario.GESTOR,
        true,
        agora,
        agora);
  }

  @Test
  void deveConcluirSolicitacao() {
    final Usuario gestor = criarGestor();
    final Solicitacao solicitacao = criarSolicitacaoEmValidacao();

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
    when(solicitacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(atividadeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final Solicitacao resultado =
        useCase.execute(
            new EncerrarSolicitacaoUseCase.Input(
                solicitacao.getId(), true, "Concluido com sucesso", gestor.getId()));

    assertEquals(StatusSolicitacao.CONCLUIDA, resultado.getStatus());
    assertNotNull(resultado.getConcluidaEm());
    verify(eventPublisher).publish(any(SolicitacaoFinalizadaEvent.class));
    verify(eventoModeloRepository).save(any(EventoModelo.class));
  }

  @Test
  void deveCancelarSolicitacao() {
    final Usuario gestor = criarGestor();
    final Solicitacao solicitacao = criarSolicitacaoEmValidacao();

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
    when(solicitacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(atividadeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final Solicitacao resultado =
        useCase.execute(
            new EncerrarSolicitacaoUseCase.Input(
                solicitacao.getId(), false, "Cancelamento", gestor.getId()));

    assertEquals(StatusSolicitacao.CANCELADA, resultado.getStatus());
    assertNotNull(resultado.getCanceladaEm());
    verify(eventPublisher).publish(any(SolicitacaoFinalizadaEvent.class));
    verify(eventoModeloRepository, never()).save(any(EventoModelo.class));
  }

  @Test
  void deveFalharSeOperadorTentaEncerrar() {
    final Instant agora = Instant.now();
    final Usuario operador =
        new Usuario(
            UUID.randomUUID(),
            "Op",
            "op@test.com",
            "hash",
            PerfilUsuario.OPERADOR,
            true,
            agora,
            agora);

    when(usuarioRepository.findById(operador.getId())).thenReturn(Optional.of(operador));

    assertThrows(
        NaoAutorizadoException.class,
        () ->
            useCase.execute(
                new EncerrarSolicitacaoUseCase.Input(
                    UUID.randomUUID(), true, "Teste", operador.getId())));
  }
}
