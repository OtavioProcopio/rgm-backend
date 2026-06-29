package com.rgm.api.core.application.usecases.solicitacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.events.SolicitacaoFinalizadaEvent;
import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.entities.SolicitacaoAtribuicao;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.DomainEventPublisher;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CancelarSolicitacaoUseCaseTest {

  private SolicitacaoRepository solicitacaoRepository;
  private UsuarioRepository usuarioRepository;
  private AtividadeSolicitacaoRepository atividadeRepository;
  private SolicitacaoAtribuicaoRepository atribuicaoRepository;
  private DomainEventPublisher eventPublisher;
  private CancelarSolicitacaoUseCase useCase;

  private static final Instant NOW = Instant.now();

  @BeforeEach
  void setUp() {
    solicitacaoRepository = mock(SolicitacaoRepository.class);
    usuarioRepository = mock(UsuarioRepository.class);
    atividadeRepository = mock(AtividadeSolicitacaoRepository.class);
    atribuicaoRepository = mock(SolicitacaoAtribuicaoRepository.class);
    eventPublisher = mock(DomainEventPublisher.class);
    useCase =
        new CancelarSolicitacaoUseCase(
            solicitacaoRepository,
            usuarioRepository,
            atividadeRepository,
            atribuicaoRepository,
            eventPublisher);
  }

  private Solicitacao criarSolicitacao(final StatusSolicitacao status, final UUID abertaPorId) {
    return new Solicitacao(
        UUID.randomUUID(),
        "Titulo",
        "Descricao",
        TipoSolicitacao.REPARO,
        status,
        status.exigePrioridade() ? PrioridadeSolicitacao.MEDIA : null,
        UUID.randomUUID(),
        abertaPorId,
        null,
        NOW,
        NOW,
        null,
        null);
  }

  private Usuario criarUsuario(final PerfilUsuario perfil) {
    return new Usuario(
        UUID.randomUUID(), "Nome Usuario", "user@test.com", "hash", perfil, true, NOW, NOW);
  }

  @Test
  void deveCancelarSolicitacaoComSucesso() {
    final Usuario gestor = criarUsuario(PerfilUsuario.GESTOR);
    final Solicitacao solicitacao =
        criarSolicitacao(StatusSolicitacao.EM_ANDAMENTO, UUID.randomUUID());

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
    final Solicitacao solicitacao =
        criarSolicitacao(StatusSolicitacao.EM_ANDAMENTO, UUID.randomUUID());

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));

    assertThrows(
        ValidationException.class,
        () ->
            useCase.execute(
                new CancelarSolicitacaoUseCase.Input(solicitacao.getId(), "", gestor.getId())));
  }

  @Test
  void deveFalharSeOperadorTentaCancelarEmAndamento() {
    final Usuario operador = criarUsuario(PerfilUsuario.OPERADOR);
    final Solicitacao solicitacao =
        criarSolicitacao(StatusSolicitacao.EM_ANDAMENTO, operador.getId());

    when(usuarioRepository.findById(operador.getId())).thenReturn(Optional.of(operador));
    when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));

    assertThrows(
        NaoAutorizadoException.class,
        () ->
            useCase.execute(
                new CancelarSolicitacaoUseCase.Input(
                    solicitacao.getId(), "Cancelando", operador.getId())));
  }

  @Test
  void devePermitirOperadorCancelarPropriaEmAFazerSemResponsavel() {
    final Usuario operador = criarUsuario(PerfilUsuario.OPERADOR);
    final Solicitacao solicitacao = criarSolicitacao(StatusSolicitacao.A_FAZER, operador.getId());

    when(usuarioRepository.findById(operador.getId())).thenReturn(Optional.of(operador));
    when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
    when(atribuicaoRepository.findBySolicitacaoId(solicitacao.getId())).thenReturn(List.of());
    when(solicitacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(atividadeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final Solicitacao resultado =
        useCase.execute(
            new CancelarSolicitacaoUseCase.Input(
                solicitacao.getId(), "Abri por engano", operador.getId()));

    assertEquals(StatusSolicitacao.CANCELADA, resultado.getStatus());
  }

  @Test
  void deveFalharSeOperadorTentaCancelarEmAFazerComResponsavel() {
    final Usuario operador = criarUsuario(PerfilUsuario.OPERADOR);
    final Solicitacao solicitacao = criarSolicitacao(StatusSolicitacao.A_FAZER, operador.getId());
    final SolicitacaoAtribuicao atribuicao = mock(SolicitacaoAtribuicao.class);
    when(atribuicao.getRemovidoEm()).thenReturn(null);

    when(usuarioRepository.findById(operador.getId())).thenReturn(Optional.of(operador));
    when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
    when(atribuicaoRepository.findBySolicitacaoId(solicitacao.getId()))
        .thenReturn(List.of(atribuicao));

    assertThrows(
        BusinessRuleException.class,
        () ->
            useCase.execute(
                new CancelarSolicitacaoUseCase.Input(
                    solicitacao.getId(), "Tentando cancelar", operador.getId())));
  }

  @Test
  void deveFalharSeOperadorTentaCancelarSolicitacaoDeOutroEmAFazer() {
    final Usuario operador = criarUsuario(PerfilUsuario.OPERADOR);
    final Solicitacao solicitacao =
        criarSolicitacao(StatusSolicitacao.A_FAZER, UUID.randomUUID()); // outro abriu

    when(usuarioRepository.findById(operador.getId())).thenReturn(Optional.of(operador));
    when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));

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
                new CancelarSolicitacaoUseCase.Input(solicitacaoId, "Cancelando", gestor.getId())));
  }
}
