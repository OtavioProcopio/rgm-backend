package com.rgm.api.core.application.usecases.solicitacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RegistrarComentarioUseCaseTest {

  private SolicitacaoRepository solicitacaoRepository;
  private AtividadeSolicitacaoRepository atividadeRepository;
  private UsuarioRepository usuarioRepository;
  private SolicitacaoAtribuicaoRepository atribuicaoRepository;
  private RegistrarComentarioUseCase useCase;

  @BeforeEach
  void setUp() {
    solicitacaoRepository = mock(SolicitacaoRepository.class);
    atividadeRepository = mock(AtividadeSolicitacaoRepository.class);
    usuarioRepository = mock(UsuarioRepository.class);
    atribuicaoRepository = mock(SolicitacaoAtribuicaoRepository.class);
    useCase =
        new RegistrarComentarioUseCase(
            solicitacaoRepository,
            atividadeRepository,
            usuarioRepository,
            atribuicaoRepository);
  }

  @Test
  void deveRegistrarComentarioComSucesso() {
    final Instant agora = Instant.now();
    final UUID abertaPorId = UUID.randomUUID();
    final Solicitacao solicitacao =
        new Solicitacao(
            UUID.randomUUID(),
            "T",
            "D",
            TipoSolicitacao.REPARO,
            StatusSolicitacao.EM_ANDAMENTO,
            PrioridadeSolicitacao.ALTA,
            UUID.randomUUID(),
            abertaPorId,
            null,
            agora,
            agora,
            null,
            null);

    final UUID autorId = UUID.randomUUID();
    final Usuario autor =
        new Usuario(
            autorId,
            "Autor",
            "autor@rgm.com",
            "hash",
            PerfilUsuario.GESTOR,
            true,
            agora,
            agora);

    when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
    when(usuarioRepository.findById(autorId)).thenReturn(Optional.of(autor));
    when(atividadeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final AtividadeSolicitacao resultado =
        useCase.execute(
            new RegistrarComentarioUseCase.Input(solicitacao.getId(), "Comentario", autorId));

    assertNotNull(resultado);
    verify(atividadeRepository).save(any(AtividadeSolicitacao.class));
  }

  @Test
  void deveFalharComComentarioVazio() {
    final Instant agora = Instant.now();
    final UUID abertaPorId = UUID.randomUUID();
    final Solicitacao solicitacao =
        new Solicitacao(
            UUID.randomUUID(),
            "T",
            "D",
            TipoSolicitacao.REPARO,
            StatusSolicitacao.EM_ANDAMENTO,
            PrioridadeSolicitacao.ALTA,
            UUID.randomUUID(),
            abertaPorId,
            null,
            agora,
            agora,
            null,
            null);

    final UUID autorId = UUID.randomUUID();
    final Usuario autor =
        new Usuario(
            autorId,
            "Autor",
            "autor@rgm.com",
            "hash",
            PerfilUsuario.GESTOR,
            true,
            agora,
            agora);

    when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
    when(usuarioRepository.findById(autorId)).thenReturn(Optional.of(autor));

    assertThrows(
        ValidationException.class,
        () ->
            useCase.execute(
                new RegistrarComentarioUseCase.Input(solicitacao.getId(), "", autorId)));
  }

  @Test
  void deveFalharComSolicitacaoNaoEncontrada() {
    final UUID id = UUID.randomUUID();
    when(solicitacaoRepository.findById(id)).thenReturn(Optional.empty());

    assertThrows(
        RecursoNaoEncontradoException.class,
        () ->
            useCase.execute(
                new RegistrarComentarioUseCase.Input(id, "Comentario", UUID.randomUUID())));
  }

  @Test
  void deveFalharQuandoUsuarioNaoEncontrado() {
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

    final UUID autorId = UUID.randomUUID();
    when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
    when(usuarioRepository.findById(autorId)).thenReturn(Optional.empty());

    assertThrows(
        RecursoNaoEncontradoException.class,
        () ->
            useCase.execute(
                new RegistrarComentarioUseCase.Input(solicitacao.getId(), "Comentario", autorId)));
  }

  @Test
  void deveFalharQuandoUsuarioInativo() {
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

    final UUID autorId = UUID.randomUUID();
    final Usuario autor =
        new Usuario(
            autorId,
            "Autor",
            "autor@rgm.com",
            "hash",
            PerfilUsuario.GESTOR,
            false,
            agora,
            agora);

    when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
    when(usuarioRepository.findById(autorId)).thenReturn(Optional.of(autor));

    assertThrows(
        NaoAutorizadoException.class,
        () ->
            useCase.execute(
                new RegistrarComentarioUseCase.Input(solicitacao.getId(), "Comentario", autorId)));
  }

  @Test
  void deveFalharQuandoUsuarioPerfilExterno() {
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

    final UUID autorId = UUID.randomUUID();
    final Usuario autor =
        new Usuario(
            autorId,
            "Autor",
            null,
            null,
            PerfilUsuario.EXTERNO,
            true,
            agora,
            agora);

    when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
    when(usuarioRepository.findById(autorId)).thenReturn(Optional.of(autor));

    assertThrows(
        NaoAutorizadoException.class,
        () ->
            useCase.execute(
                new RegistrarComentarioUseCase.Input(solicitacao.getId(), "Comentario", autorId)));
  }

  @Test
  void devePermitirOperadorSeForAutorComentar() {
    final Instant agora = Instant.now();
    final UUID operadorId = UUID.randomUUID();
    final Solicitacao solicitacao =
        new Solicitacao(
            UUID.randomUUID(),
            "T",
            "D",
            TipoSolicitacao.REPARO,
            StatusSolicitacao.EM_ANDAMENTO,
            PrioridadeSolicitacao.ALTA,
            UUID.randomUUID(),
            operadorId,
            null,
            agora,
            agora,
            null,
            null);

    final Usuario operador =
        new Usuario(
            operadorId,
            "Operador",
            "op@rgm.com",
            "hash",
            PerfilUsuario.OPERADOR,
            true,
            agora,
            agora);

    when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
    when(usuarioRepository.findById(operadorId)).thenReturn(Optional.of(operador));
    when(atividadeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final AtividadeSolicitacao resultado =
        useCase.execute(
            new RegistrarComentarioUseCase.Input(solicitacao.getId(), "Comentario", operadorId));

    assertNotNull(resultado);
  }

  @Test
  void devePermitirOperadorSeForAtribuidoComentar() {
    final Instant agora = Instant.now();
    final UUID operadorId = UUID.randomUUID();
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

    final Usuario operador =
        new Usuario(
            operadorId,
            "Operador",
            "op@rgm.com",
            "hash",
            PerfilUsuario.OPERADOR,
            true,
            agora,
            agora);

    when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
    when(usuarioRepository.findById(operadorId)).thenReturn(Optional.of(operador));
    when(atribuicaoRepository.existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(
            solicitacao.getId(), operadorId))
        .thenReturn(true);
    when(atividadeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final AtividadeSolicitacao resultado =
        useCase.execute(
            new RegistrarComentarioUseCase.Input(solicitacao.getId(), "Comentario", operadorId));

    assertNotNull(resultado);
  }

  @Test
  void deveFalharQuandoOperadorNaoAutorNemAtribuidoComentar() {
    final Instant agora = Instant.now();
    final UUID operadorId = UUID.randomUUID();
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

    final Usuario operador =
        new Usuario(
            operadorId,
            "Operador",
            "op@rgm.com",
            "hash",
            PerfilUsuario.OPERADOR,
            true,
            agora,
            agora);

    when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
    when(usuarioRepository.findById(operadorId)).thenReturn(Optional.of(operador));
    when(atribuicaoRepository.existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(
            solicitacao.getId(), operadorId))
        .thenReturn(false);

    assertThrows(
        NaoAutorizadoException.class,
        () ->
            useCase.execute(
                new RegistrarComentarioUseCase.Input(solicitacao.getId(), "Comentario", operadorId)));
  }

  @Test
  void deveFalharQuandoSolicitacaoTerminalEOperadorComentar() {
    final Instant agora = Instant.now();
    final UUID operadorId = UUID.randomUUID();
    final Solicitacao solicitacao =
        new Solicitacao(
            UUID.randomUUID(),
            "T",
            "D",
            TipoSolicitacao.REPARO,
            StatusSolicitacao.CONCLUIDA,
            PrioridadeSolicitacao.ALTA,
            UUID.randomUUID(),
            operadorId,
            "Comentario final",
            agora,
            agora,
            agora,
            null);

    final Usuario operador =
        new Usuario(
            operadorId,
            "Operador",
            "op@rgm.com",
            "hash",
            PerfilUsuario.OPERADOR,
            true,
            agora,
            agora);

    when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
    when(usuarioRepository.findById(operadorId)).thenReturn(Optional.of(operador));

    assertThrows(
        NaoAutorizadoException.class,
        () ->
            useCase.execute(
                new RegistrarComentarioUseCase.Input(solicitacao.getId(), "Comentario", operadorId)));
  }

  @Test
  void devePermitirGestorComentarEmSolicitacaoTerminal() {
    final Instant agora = Instant.now();
    final Solicitacao solicitacao =
        new Solicitacao(
            UUID.randomUUID(),
            "T",
            "D",
            TipoSolicitacao.REPARO,
            StatusSolicitacao.CONCLUIDA,
            PrioridadeSolicitacao.ALTA,
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Comentario final",
            agora,
            agora,
            agora,
            null);

    final UUID gestorId = UUID.randomUUID();
    final Usuario gestor =
        new Usuario(
            gestorId,
            "Gestor",
            "gestor@rgm.com",
            "hash",
            PerfilUsuario.GESTOR,
            true,
            agora,
            agora);

    when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
    when(usuarioRepository.findById(gestorId)).thenReturn(Optional.of(gestor));
    when(atividadeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final AtividadeSolicitacao resultado =
        useCase.execute(
            new RegistrarComentarioUseCase.Input(solicitacao.getId(), "Comentario", gestorId));

    assertNotNull(resultado);
  }
}
