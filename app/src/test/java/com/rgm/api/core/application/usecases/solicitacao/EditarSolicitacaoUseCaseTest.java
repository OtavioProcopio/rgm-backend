package com.rgm.api.core.application.usecases.solicitacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EditarSolicitacaoUseCaseTest {

  private SolicitacaoRepository solicitacaoRepository;
  private UsuarioRepository usuarioRepository;
  private SolicitacaoAtribuicaoRepository atribuicaoRepository;
  private EditarSolicitacaoUseCase useCase;

  private static final Instant NOW = Instant.now();

  @BeforeEach
  void setUp() {
    solicitacaoRepository = mock(SolicitacaoRepository.class);
    usuarioRepository = mock(UsuarioRepository.class);
    atribuicaoRepository = mock(SolicitacaoAtribuicaoRepository.class);
    useCase =
        new EditarSolicitacaoUseCase(
            solicitacaoRepository, usuarioRepository, atribuicaoRepository);
  }

  private Usuario criarGestor() {
    return new Usuario(
        UUID.randomUUID(), "Gestor", "g@x.com", "hash", PerfilUsuario.GESTOR, true, NOW, NOW);
  }

  private Usuario criarOperador() {
    return new Usuario(
        UUID.randomUUID(), "Op", "op@x.com", "hash", PerfilUsuario.OPERADOR, true, NOW, NOW);
  }

  private Usuario criarExterno() {
    return new Usuario(UUID.randomUUID(), "Ext", null, null, PerfilUsuario.EXTERNO, true, NOW, NOW);
  }

  private Solicitacao criarSolicitacaoAFazer(final UUID abertaPorId) {
    return new Solicitacao(
        UUID.randomUUID(),
        "Titulo",
        "Descricao",
        TipoSolicitacao.REPARO,
        StatusSolicitacao.A_FAZER,
        null,
        UUID.randomUUID(),
        abertaPorId,
        null,
        NOW,
        NOW,
        null,
        null);
  }

  @Test
  void execute_gestorPodeEditarQualquerSolicitacao() {
    final Usuario gestor = criarGestor();
    final Solicitacao sol = criarSolicitacaoAFazer(UUID.randomUUID());
    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(solicitacaoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    final Solicitacao result =
        useCase.execute(
            new EditarSolicitacaoUseCase.Input(
                sol.getId(), "Novo titulo", "Nova desc", gestor.getId()));

    assertEquals("Novo titulo", result.getTitulo());
    assertEquals("Nova desc", result.getDescricao());
  }

  @Test
  void execute_operadorPodeEditarSolicitacaoQueAbriu() {
    final Usuario operador = criarOperador();
    final Solicitacao sol = criarSolicitacaoAFazer(operador.getId());
    when(usuarioRepository.findById(operador.getId())).thenReturn(Optional.of(operador));
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(solicitacaoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    final Solicitacao result =
        useCase.execute(
            new EditarSolicitacaoUseCase.Input(sol.getId(), "T", "D", operador.getId()));

    assertEquals("T", result.getTitulo());
  }

  @Test
  void execute_operadorAtribuidoPodeEditar() {
    final Usuario operador = criarOperador();
    final Solicitacao sol = criarSolicitacaoAFazer(UUID.randomUUID()); // não foi ele que abriu
    when(usuarioRepository.findById(operador.getId())).thenReturn(Optional.of(operador));
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(atribuicaoRepository.existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(
            sol.getId(), operador.getId()))
        .thenReturn(true);
    when(solicitacaoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    final Solicitacao result =
        useCase.execute(
            new EditarSolicitacaoUseCase.Input(sol.getId(), "T", "D", operador.getId()));

    assertNotNull(result);
  }

  @Test
  void execute_operadorNaoAtribuidoNaoAbriuLancaException() {
    final Usuario operador = criarOperador();
    final Solicitacao sol = criarSolicitacaoAFazer(UUID.randomUUID());
    when(usuarioRepository.findById(operador.getId())).thenReturn(Optional.of(operador));
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(atribuicaoRepository.existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(any(), any()))
        .thenReturn(false);

    assertThrows(
        NaoAutorizadoException.class,
        () ->
            useCase.execute(
                new EditarSolicitacaoUseCase.Input(sol.getId(), "T", "D", operador.getId())));
  }

  @Test
  void execute_externoNaoPodeEditar() {
    final Usuario externo = criarExterno();
    final Solicitacao sol = criarSolicitacaoAFazer(UUID.randomUUID());
    when(usuarioRepository.findById(externo.getId())).thenReturn(Optional.of(externo));
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));

    assertThrows(
        NaoAutorizadoException.class,
        () ->
            useCase.execute(
                new EditarSolicitacaoUseCase.Input(sol.getId(), "T", "D", externo.getId())));
  }

  @Test
  void execute_usuarioNaoEncontradoLancaException() {
    final UUID userId = UUID.randomUUID();
    when(usuarioRepository.findById(userId)).thenReturn(Optional.empty());

    assertThrows(
        RecursoNaoEncontradoException.class,
        () ->
            useCase.execute(
                new EditarSolicitacaoUseCase.Input(UUID.randomUUID(), "T", "D", userId)));
  }

  @Test
  void execute_solicitacaoNaoEncontradaLancaException() {
    final Usuario gestor = criarGestor();
    final UUID solId = UUID.randomUUID();
    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(solicitacaoRepository.findById(solId)).thenReturn(Optional.empty());

    assertThrows(
        RecursoNaoEncontradoException.class,
        () -> useCase.execute(new EditarSolicitacaoUseCase.Input(solId, "T", "D", gestor.getId())));
  }
}
