package com.rgm.api.core.application.usecases.solicitacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoAtividadeSolicitacao;
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

class TriarSolicitacaoUseCaseTest {

  private SolicitacaoRepository solicitacaoRepository;
  private UsuarioRepository usuarioRepository;
  private SolicitacaoAtribuicaoRepository atribuicaoRepository;
  private AtividadeSolicitacaoRepository atividadeRepository;
  private TriarSolicitacaoUseCase useCase;

  @BeforeEach
  void setUp() {
    solicitacaoRepository = mock(SolicitacaoRepository.class);
    usuarioRepository = mock(UsuarioRepository.class);
    atribuicaoRepository = mock(SolicitacaoAtribuicaoRepository.class);
    atividadeRepository = mock(AtividadeSolicitacaoRepository.class);
    useCase =
        new TriarSolicitacaoUseCase(
            solicitacaoRepository, usuarioRepository, atribuicaoRepository, atividadeRepository);
  }

  private Usuario criarUsuario(final PerfilUsuario perfil) {
    final Instant agora = Instant.now();
    if (perfil == PerfilUsuario.EXTERNO) {
      return new Usuario(UUID.randomUUID(), "Externo", null, null, perfil, true, agora, agora);
    }
    return new Usuario(
        UUID.randomUUID(), "User", "user@test.com", "hash", perfil, true, agora, agora);
  }

  private Solicitacao criarSolicitacao() {
    final Instant agora = Instant.now();
    return new Solicitacao(
        UUID.randomUUID(),
        "Titulo",
        "Descricao",
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
  }

  @Test
  void deveTriarComSucesso() {
    final Usuario gestor = criarUsuario(PerfilUsuario.GESTOR);
    final Usuario operador = criarUsuario(PerfilUsuario.OPERADOR);
    final Solicitacao solicitacao = criarSolicitacao();

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
    when(usuarioRepository.findAllByIdIn(List.of(operador.getId()))).thenReturn(List.of(operador));
    when(solicitacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(atribuicaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(atividadeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final Solicitacao resultado =
        useCase.execute(
            new TriarSolicitacaoUseCase.Input(
                solicitacao.getId(),
                PrioridadeSolicitacao.ALTA,
                List.of(operador.getId()),
                "Trocar o gabarito",
                gestor.getId()));

    assertEquals(StatusSolicitacao.EM_ANDAMENTO, resultado.getStatus());
    assertEquals(PrioridadeSolicitacao.ALTA, resultado.getPrioridade());
    verify(atribuicaoRepository, times(1)).save(any());
    verify(atividadeRepository, times(3)).save(any());
  }

  @Test
  void shouldTreatRepeatedIdsAsASingleResponsavelWhenTriaging() {
    // Arrange
    final Usuario gestor = criarUsuario(PerfilUsuario.GESTOR);
    final Usuario operador = criarUsuario(PerfilUsuario.OPERADOR);
    final Solicitacao solicitacao = criarSolicitacao();
    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
    when(usuarioRepository.findAllByIdIn(List.of(operador.getId()))).thenReturn(List.of(operador));
    when(solicitacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(atribuicaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(atividadeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // Act
    useCase.execute(
        new TriarSolicitacaoUseCase.Input(
            solicitacao.getId(),
            PrioridadeSolicitacao.ALTA,
            List.of(operador.getId(), operador.getId()),
            "Trocar o gabarito",
            gestor.getId()));

    // Assert
    verify(usuarioRepository, times(1)).findAllByIdIn(List.of(operador.getId()));
    verify(atribuicaoRepository, times(1)).save(any());
  }

  @Test
  void deveFalharSeOperadorTentaTriar() {
    final Usuario operador = criarUsuario(PerfilUsuario.OPERADOR);
    when(usuarioRepository.findById(operador.getId())).thenReturn(Optional.of(operador));

    assertThrows(
        NaoAutorizadoException.class,
        () ->
            useCase.execute(
                new TriarSolicitacaoUseCase.Input(
                    UUID.randomUUID(),
                    PrioridadeSolicitacao.ALTA,
                    List.of(UUID.randomUUID()),
                    "Trocar o gabarito",
                    operador.getId())));
  }

  @Test
  void deveFalharSemResponsaveis() {
    final Usuario gestor = criarUsuario(PerfilUsuario.GESTOR);
    final Solicitacao solicitacao = criarSolicitacao();

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));

    assertThrows(
        BusinessRuleException.class,
        () ->
            useCase.execute(
                new TriarSolicitacaoUseCase.Input(
                    solicitacao.getId(),
                    PrioridadeSolicitacao.ALTA,
                    List.of(),
                    "Trocar o gabarito",
                    gestor.getId())));
  }

  @Test
  void deveFalharComResponsavelInativo() {
    final Usuario gestor = criarUsuario(PerfilUsuario.GESTOR);
    final Instant agora = Instant.now();
    final Usuario inativo =
        new Usuario(
            UUID.randomUUID(),
            "Inativo",
            "in@test.com",
            "hash",
            PerfilUsuario.OPERADOR,
            false,
            agora,
            agora);
    final Solicitacao solicitacao = criarSolicitacao();

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
    when(usuarioRepository.findAllByIdIn(List.of(inativo.getId()))).thenReturn(List.of(inativo));

    assertThrows(
        BusinessRuleException.class,
        () ->
            useCase.execute(
                new TriarSolicitacaoUseCase.Input(
                    solicitacao.getId(),
                    PrioridadeSolicitacao.ALTA,
                    List.of(inativo.getId()),
                    "Trocar o gabarito",
                    gestor.getId())));
  }

  @Test
  void deveFalharComResponsavelAdministrador() {
    final Usuario gestor = criarUsuario(PerfilUsuario.GESTOR);
    final Usuario admin = criarUsuario(PerfilUsuario.ADMINISTRADOR);
    final Solicitacao solicitacao = criarSolicitacao();

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
    when(usuarioRepository.findAllByIdIn(List.of(admin.getId()))).thenReturn(List.of(admin));

    assertThrows(
        BusinessRuleException.class,
        () ->
            useCase.execute(
                new TriarSolicitacaoUseCase.Input(
                    solicitacao.getId(),
                    PrioridadeSolicitacao.ALTA,
                    List.of(admin.getId()),
                    "Trocar o gabarito",
                    gestor.getId())));
  }

  @Test
  void shouldFailAndWriteNothingWhenDescricaoIsBlank() {
    // Arrange
    final Usuario gestor = criarUsuario(PerfilUsuario.GESTOR);
    final Solicitacao solicitacao = criarSolicitacao();

    // Act
    final var thrown =
        assertThrows(
            BusinessRuleException.class,
            () ->
                useCase.execute(
                    new TriarSolicitacaoUseCase.Input(
                        solicitacao.getId(),
                        PrioridadeSolicitacao.ALTA,
                        List.of(UUID.randomUUID()),
                        "   ",
                        gestor.getId())));

    // Assert
    assertNotNull(thrown.getMessage());
    verifyNoInteractions(solicitacaoRepository, atribuicaoRepository, atividadeRepository);
  }

  @Test
  void shouldFailWhenDescricaoIsNull() {
    // Arrange
    final Usuario gestor = criarUsuario(PerfilUsuario.GESTOR);

    // Act + Assert
    assertThrows(
        BusinessRuleException.class,
        () ->
            useCase.execute(
                new TriarSolicitacaoUseCase.Input(
                    UUID.randomUUID(),
                    PrioridadeSolicitacao.ALTA,
                    List.of(UUID.randomUUID()),
                    null,
                    gestor.getId())));
    verifyNoInteractions(solicitacaoRepository, atribuicaoRepository, atividadeRepository);
  }

  @Test
  void shouldRecordTrimmedDescricaoAsComentarioOfGestorWhenTriaging() {
    // Arrange
    final Usuario gestor = criarUsuario(PerfilUsuario.GESTOR);
    final Usuario operador = criarUsuario(PerfilUsuario.OPERADOR);
    final Solicitacao solicitacao = criarSolicitacao();
    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
    when(usuarioRepository.findAllByIdIn(List.of(operador.getId()))).thenReturn(List.of(operador));
    when(solicitacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(atribuicaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(atividadeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // Act
    useCase.execute(
        new TriarSolicitacaoUseCase.Input(
            solicitacao.getId(),
            PrioridadeSolicitacao.ALTA,
            List.of(operador.getId()),
            "  Trocar o gabarito  ",
            gestor.getId()));

    // Assert
    final var captor = org.mockito.ArgumentCaptor.forClass(AtividadeSolicitacao.class);
    verify(atividadeRepository, times(3)).save(captor.capture());
    final var comentario =
        captor.getAllValues().stream()
            .filter(a -> a.getTipo() == TipoAtividadeSolicitacao.COMENTARIO)
            .findFirst()
            .orElseThrow();
    assertEquals("Trocar o gabarito", comentario.getComentario());
    assertEquals(gestor.getId(), comentario.getAutorUsuarioId());
  }
}
