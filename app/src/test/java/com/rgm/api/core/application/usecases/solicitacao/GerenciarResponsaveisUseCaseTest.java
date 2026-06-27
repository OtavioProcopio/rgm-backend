package com.rgm.api.core.application.usecases.solicitacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GerenciarResponsaveisUseCaseTest {

  private SolicitacaoRepository solicitacaoRepository;
  private UsuarioRepository usuarioRepository;
  private SolicitacaoAtribuicaoRepository atribuicaoRepository;
  private AtividadeSolicitacaoRepository atividadeRepository;
  private GerenciarResponsaveisUseCase useCase;

  @BeforeEach
  void setUp() {
    solicitacaoRepository = mock(SolicitacaoRepository.class);
    usuarioRepository = mock(UsuarioRepository.class);
    atribuicaoRepository = mock(SolicitacaoAtribuicaoRepository.class);
    atividadeRepository = mock(AtividadeSolicitacaoRepository.class);
    useCase =
        new GerenciarResponsaveisUseCase(
            solicitacaoRepository, usuarioRepository, atribuicaoRepository, atividadeRepository);
  }

  private Solicitacao criarSolicitacao(final StatusSolicitacao status) {
    final Instant agora = Instant.now();
    final String comentarioFinal =
        (status == StatusSolicitacao.CONCLUIDA || status == StatusSolicitacao.CANCELADA)
            ? "Comentario"
            : null;
    return new Solicitacao(
        UUID.randomUUID(),
        "Titulo",
        "Desc",
        TipoSolicitacao.REPARO,
        status,
        status.exigePrioridade() ? PrioridadeSolicitacao.ALTA : null,
        UUID.randomUUID(),
        UUID.randomUUID(),
        comentarioFinal,
        agora,
        agora,
        status == StatusSolicitacao.CONCLUIDA ? agora : null,
        status == StatusSolicitacao.CANCELADA ? agora : null);
  }

  private Usuario criarUsuario(final String nome, final PerfilUsuario perfil, final boolean ativo) {
    final var u =
        perfil == PerfilUsuario.EXTERNO
            ? Usuario.criarExterno(nome, Instant.now())
            : Usuario.criarInterno(nome, nome + "@rgm.com", "hash", perfil, Instant.now());
    if (!ativo) {
      return u.withAtivo(false, Instant.now());
    }
    return u;
  }

  @Test
  void deveFalharSeGestorNaoForEncontrado() {
    when(usuarioRepository.findById(any())).thenReturn(Optional.empty());

    assertThrows(
        RecursoNaoEncontradoException.class,
        () ->
            useCase.execute(
                new GerenciarResponsaveisUseCase.Input(
                    UUID.randomUUID(), List.of(UUID.randomUUID()), UUID.randomUUID())));
  }

  @Test
  void deveFalharSeUsuarioNaoForGestorOuAdmin() {
    final UUID gestorId = UUID.randomUUID();
    final Usuario op = criarUsuario("Operator", PerfilUsuario.OPERADOR, true);
    when(usuarioRepository.findById(gestorId)).thenReturn(Optional.of(op));

    assertThrows(
        NaoAutorizadoException.class,
        () ->
            useCase.execute(
                new GerenciarResponsaveisUseCase.Input(
                    UUID.randomUUID(), List.of(UUID.randomUUID()), gestorId)));
  }

  @Test
  void deveFalharSeSolicitacaoNaoForEncontrada() {
    final UUID gestorId = UUID.randomUUID();
    final Usuario gestor = criarUsuario("Gestor", PerfilUsuario.GESTOR, true);
    when(usuarioRepository.findById(gestorId)).thenReturn(Optional.of(gestor));
    when(solicitacaoRepository.findById(any())).thenReturn(Optional.empty());

    assertThrows(
        RecursoNaoEncontradoException.class,
        () ->
            useCase.execute(
                new GerenciarResponsaveisUseCase.Input(
                    UUID.randomUUID(), List.of(UUID.randomUUID()), gestorId)));
  }

  @Test
  void deveFalharSeSolicitacaoEstiverEncerrada() {
    final UUID gestorId = UUID.randomUUID();
    final Usuario gestor = criarUsuario("Gestor", PerfilUsuario.GESTOR, true);
    final Solicitacao sol = criarSolicitacao(StatusSolicitacao.CONCLUIDA);

    when(usuarioRepository.findById(gestorId)).thenReturn(Optional.of(gestor));
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));

    assertThrows(
        BusinessRuleException.class,
        () ->
            useCase.execute(
                new GerenciarResponsaveisUseCase.Input(
                    sol.getId(), List.of(UUID.randomUUID()), gestorId)));
  }

  @Test
  void deveFalharSeListaDeResponsaveisForVazia() {
    final UUID gestorId = UUID.randomUUID();
    final Usuario gestor = criarUsuario("Gestor", PerfilUsuario.GESTOR, true);
    final Solicitacao sol = criarSolicitacao(StatusSolicitacao.EM_ANDAMENTO);

    when(usuarioRepository.findById(gestorId)).thenReturn(Optional.of(gestor));
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));

    assertThrows(
        BusinessRuleException.class,
        () -> useCase.execute(new GerenciarResponsaveisUseCase.Input(sol.getId(), null, gestorId)));

    assertThrows(
        BusinessRuleException.class,
        () ->
            useCase.execute(
                new GerenciarResponsaveisUseCase.Input(sol.getId(), List.of(), gestorId)));
  }

  @Test
  void deveFalharSeResponsavelNaoForEncontrado() {
    final UUID gestorId = UUID.randomUUID();
    final Usuario gestor = criarUsuario("Gestor", PerfilUsuario.GESTOR, true);
    final Solicitacao sol = criarSolicitacao(StatusSolicitacao.EM_ANDAMENTO);
    final UUID respId = UUID.randomUUID();

    when(usuarioRepository.findById(gestorId)).thenReturn(Optional.of(gestor));
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findAllByIdIn(any())).thenReturn(List.of());

    assertThrows(
        RecursoNaoEncontradoException.class,
        () ->
            useCase.execute(
                new GerenciarResponsaveisUseCase.Input(sol.getId(), List.of(respId), gestorId)));
  }

  @Test
  void deveFalharSeResponsavelEstiverInativo() {
    final UUID gestorId = UUID.randomUUID();
    final Usuario gestor = criarUsuario("Gestor", PerfilUsuario.GESTOR, true);
    final Solicitacao sol = criarSolicitacao(StatusSolicitacao.EM_ANDAMENTO);
    final Usuario respInativo = criarUsuario("Resp", PerfilUsuario.OPERADOR, false);

    when(usuarioRepository.findById(gestorId)).thenReturn(Optional.of(gestor));
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findAllByIdIn(any())).thenReturn(List.of(respInativo));

    assertThrows(
        BusinessRuleException.class,
        () ->
            useCase.execute(
                new GerenciarResponsaveisUseCase.Input(
                    sol.getId(), List.of(respInativo.getId()), gestorId)));
  }

  @Test
  void deveFalharSeResponsavelNaoForAtribuivel() {
    final UUID gestorId = UUID.randomUUID();
    final Usuario gestor = criarUsuario("Gestor", PerfilUsuario.GESTOR, true);
    final Solicitacao sol = criarSolicitacao(StatusSolicitacao.EM_ANDAMENTO);
    final Usuario respAdmin = criarUsuario("Resp", PerfilUsuario.ADMINISTRADOR, true);

    when(usuarioRepository.findById(gestorId)).thenReturn(Optional.of(gestor));
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findAllByIdIn(any())).thenReturn(List.of(respAdmin));

    assertThrows(
        BusinessRuleException.class,
        () ->
            useCase.execute(
                new GerenciarResponsaveisUseCase.Input(
                    sol.getId(), List.of(respAdmin.getId()), gestorId)));
  }

  @Test
  void deveGerenciarResponsaveisComSucesso() {
    final UUID gestorId = UUID.randomUUID();
    final Usuario gestor = criarUsuario("Gestor", PerfilUsuario.GESTOR, true);
    final Solicitacao sol = criarSolicitacao(StatusSolicitacao.EM_ANDAMENTO);

    final Usuario respMantido = criarUsuario("Mantido", PerfilUsuario.OPERADOR, true);
    final Usuario respRemovido = criarUsuario("Removido", PerfilUsuario.OPERADOR, true);
    final Usuario respAdicionado = criarUsuario("Adicionado", PerfilUsuario.OPERADOR, true);

    final SolicitacaoAtribuicao atrMantido =
        SolicitacaoAtribuicao.criar(sol.getId(), respMantido.getId(), gestorId, Instant.now());
    final SolicitacaoAtribuicao atrRemovido =
        SolicitacaoAtribuicao.criar(sol.getId(), respRemovido.getId(), gestorId, Instant.now());

    when(usuarioRepository.findById(gestorId)).thenReturn(Optional.of(gestor));
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findAllByIdIn(any())).thenReturn(List.of(respMantido, respAdicionado));
    when(atribuicaoRepository.findBySolicitacaoId(sol.getId()))
        .thenReturn(List.of(atrMantido, atrRemovido));
    when(usuarioRepository.findById(respRemovido.getId())).thenReturn(Optional.of(respRemovido));

    final var result =
        useCase.execute(
            new GerenciarResponsaveisUseCase.Input(
                sol.getId(), List.of(respMantido.getId(), respAdicionado.getId()), gestorId));

    assertNotNull(result);
    assertEquals(sol.getId(), result.getId());

    verify(atribuicaoRepository).save(argThat(SolicitacaoAtribuicao::isAtiva)); // Novo adicionado
    verify(atribuicaoRepository).save(argThat(a -> !a.isAtiva())); // Removido
    verify(atividadeRepository, times(2)).save(any());
  }
}
