package com.rgm.api.core.application.usecases.solicitacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
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
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DevolverSolicitacaoUseCaseTest {

  private SolicitacaoRepository solicitacaoRepository;
  private UsuarioRepository usuarioRepository;
  private AtividadeSolicitacaoRepository atividadeRepository;
  private DevolverSolicitacaoUseCase useCase;

  @BeforeEach
  void setUp() {
    solicitacaoRepository = mock(SolicitacaoRepository.class);
    usuarioRepository = mock(UsuarioRepository.class);
    atividadeRepository = mock(AtividadeSolicitacaoRepository.class);
    useCase =
        new DevolverSolicitacaoUseCase(
            solicitacaoRepository, usuarioRepository, atividadeRepository);
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
  void deveDevolverComSucesso() {
    final Usuario gestor = criarGestor();
    final Solicitacao solicitacao = criarSolicitacaoEmValidacao();

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
    when(solicitacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(atividadeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final Solicitacao resultado =
        useCase.execute(
            new DevolverSolicitacaoUseCase.Input(
                solicitacao.getId(), "Necessita correcao", null, gestor.getId()));

    assertEquals(StatusSolicitacao.EM_ANDAMENTO, resultado.getStatus());
    verify(atividadeRepository, times(2)).save(any());
  }

  @Test
  void deveFalharSemMotivo() {
    assertThrows(
        ValidationException.class,
        () ->
            useCase.execute(
                new DevolverSolicitacaoUseCase.Input(
                    UUID.randomUUID(), "", null, UUID.randomUUID())));
  }

  @Test
  void deveFalharSeOperadorTentaDevolver() {
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
                new DevolverSolicitacaoUseCase.Input(
                    UUID.randomUUID(), "motivo", null, operador.getId())));
  }
}
