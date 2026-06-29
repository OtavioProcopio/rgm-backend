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
  private EditarSolicitacaoUseCase useCase;

  private static final Instant NOW = Instant.now();

  @BeforeEach
  void setUp() {
    solicitacaoRepository = mock(SolicitacaoRepository.class);
    usuarioRepository = mock(UsuarioRepository.class);
    useCase = new EditarSolicitacaoUseCase(solicitacaoRepository, usuarioRepository);
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

  private EditarSolicitacaoUseCase.Input input(
      final UUID solId, final UUID userId, final TipoSolicitacao tipo) {
    return new EditarSolicitacaoUseCase.Input(solId, "Novo titulo", "Nova desc", tipo, userId);
  }

  @Test
  void execute_gestorPodeEditarQualquerSolicitacao() {
    final Usuario gestor = criarGestor();
    final Solicitacao sol = criarSolicitacaoAFazer(UUID.randomUUID());
    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(solicitacaoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    final Solicitacao result = useCase.execute(input(sol.getId(), gestor.getId(), TipoSolicitacao.INSPECAO));

    assertEquals("Novo titulo", result.getTitulo());
    assertEquals(TipoSolicitacao.INSPECAO, result.getTipo());
  }

  @Test
  void execute_operadorPodeEditarSolicitacaoQueAbriu() {
    final Usuario operador = criarOperador();
    final Solicitacao sol = criarSolicitacaoAFazer(operador.getId());
    when(usuarioRepository.findById(operador.getId())).thenReturn(Optional.of(operador));
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(solicitacaoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    final Solicitacao result = useCase.execute(input(sol.getId(), operador.getId(), TipoSolicitacao.REPARO));

    assertEquals("Novo titulo", result.getTitulo());
  }

  @Test
  void execute_operadorNaoAbriuLancaException() {
    final Usuario operador = criarOperador();
    final Solicitacao sol = criarSolicitacaoAFazer(UUID.randomUUID()); // outro usuário abriu
    when(usuarioRepository.findById(operador.getId())).thenReturn(Optional.of(operador));
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));

    assertThrows(
        NaoAutorizadoException.class,
        () -> useCase.execute(input(sol.getId(), operador.getId(), TipoSolicitacao.REPARO)));
  }

  @Test
  void execute_externoNaoPodeEditar() {
    final Usuario externo = criarExterno();
    final Solicitacao sol = criarSolicitacaoAFazer(UUID.randomUUID());
    when(usuarioRepository.findById(externo.getId())).thenReturn(Optional.of(externo));
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));

    assertThrows(
        NaoAutorizadoException.class,
        () -> useCase.execute(input(sol.getId(), externo.getId(), TipoSolicitacao.REPARO)));
  }

  @Test
  void execute_usuarioNaoEncontradoLancaException() {
    final UUID userId = UUID.randomUUID();
    when(usuarioRepository.findById(userId)).thenReturn(Optional.empty());

    assertThrows(
        RecursoNaoEncontradoException.class,
        () ->
            useCase.execute(
                new EditarSolicitacaoUseCase.Input(
                    UUID.randomUUID(), "T", "D", TipoSolicitacao.REPARO, userId)));
  }

  @Test
  void execute_solicitacaoNaoEncontradaLancaException() {
    final Usuario gestor = criarGestor();
    final UUID solId = UUID.randomUUID();
    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(solicitacaoRepository.findById(solId)).thenReturn(Optional.empty());

    assertThrows(
        RecursoNaoEncontradoException.class,
        () ->
            useCase.execute(
                new EditarSolicitacaoUseCase.Input(
                    solId, "T", "D", TipoSolicitacao.REPARO, gestor.getId())));
  }
}
