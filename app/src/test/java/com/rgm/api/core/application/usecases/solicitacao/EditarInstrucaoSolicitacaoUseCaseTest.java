package com.rgm.api.core.application.usecases.solicitacao;

import static com.rgm.api.support.SolicitacaoTestBuilder.umaSolicitacao;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rgm.api.core.domain.exceptions.ConflitoException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoAtividadeSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EditarInstrucaoSolicitacaoUseCaseTest {

  @Mock private SolicitacaoRepository solicitacaoRepository;
  @Mock private UsuarioRepository usuarioRepository;
  @Mock private AtividadeSolicitacaoRepository atividadeRepository;

  private EditarInstrucaoSolicitacaoUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase =
        new EditarInstrucaoSolicitacaoUseCase(
            solicitacaoRepository, usuarioRepository, atividadeRepository);
  }

  private static Usuario usuario(final PerfilUsuario perfil, final boolean ativo) {
    final Instant t = Instant.parse("2026-01-01T00:00:00Z");
    return new Usuario(UUID.randomUUID(), "Nome", "u@rgm.test", "hash", perfil, ativo, t, t);
  }

  private void dado(final Usuario u, final Solicitacao s) {
    when(usuarioRepository.findById(u.getId())).thenReturn(Optional.of(u));
    if (s != null) {
      when(solicitacaoRepository.findById(s.getId())).thenReturn(Optional.of(s));
    }
  }

  @ParameterizedTest
  @EnumSource(
      value = PerfilUsuario.class,
      names = {"GESTOR", "ADMINISTRADOR"})
  void shouldSaveNewInstrucaoAndRecordThePreviousOneWhenManagerEdits(final PerfilUsuario perfil) {
    // Arrange
    final Usuario gestor = usuario(perfil, true);
    final Solicitacao atual = umaSolicitacao().emAndamento().comInstrucao("Antiga").build();
    dado(gestor, atual);
    when(solicitacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(atividadeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // Act
    final var output =
        useCase.execute(
            new EditarInstrucaoSolicitacaoUseCase.Input(
                atual.getId(), "  Nova instrucao ", gestor.getId()));

    // Assert
    assertTrue(output.alterada());
    assertEquals("Nova instrucao", output.solicitacao().getInstrucao());
    final var salva = ArgumentCaptor.forClass(Solicitacao.class);
    verify(solicitacaoRepository).save(salva.capture());
    assertEquals("Nova instrucao", salva.getValue().getInstrucao());
    final var atividade = ArgumentCaptor.forClass(AtividadeSolicitacao.class);
    verify(atividadeRepository).save(atividade.capture());
    assertEquals(TipoAtividadeSolicitacao.INSTRUCAO_ALTERADA, atividade.getValue().getTipo());
    assertEquals("Antiga", atividade.getValue().getComentario());
    assertEquals(gestor.getId(), atividade.getValue().getAutorUsuarioId());
  }

  @Test
  void shouldWriteNothingWhenTheTextIsTheSame() {
    // Arrange
    final Usuario gestor = usuario(PerfilUsuario.GESTOR, true);
    final Solicitacao atual = umaSolicitacao().emAndamento().comInstrucao("Igual").build();
    dado(gestor, atual);

    // Act
    final var output =
        useCase.execute(
            new EditarInstrucaoSolicitacaoUseCase.Input(atual.getId(), " Igual ", gestor.getId()));

    // Assert
    assertFalse(output.alterada());
    assertSame(atual, output.solicitacao());
    verify(solicitacaoRepository, never()).save(any());
    verifyNoInteractions(atividadeRepository);
  }

  @Test
  void shouldRecordAnActivityWithoutPreviousTextWhenTheSolicitacaoHadNoInstrucao() {
    // Arrange
    final Usuario gestor = usuario(PerfilUsuario.GESTOR, true);
    final Solicitacao antiga = umaSolicitacao().emAndamento().build();
    dado(gestor, antiga);
    when(solicitacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(atividadeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // Act
    useCase.execute(
        new EditarInstrucaoSolicitacaoUseCase.Input(antiga.getId(), "Primeira", gestor.getId()));

    // Assert
    final var atividade = ArgumentCaptor.forClass(AtividadeSolicitacao.class);
    verify(atividadeRepository).save(atividade.capture());
    assertEquals(null, atividade.getValue().getComentario());
  }

  @Test
  void shouldRefuseAndWriteNothingWhenTheOperatorTriesToEdit() {
    assertRefusedAndNothingWritten(usuario(PerfilUsuario.OPERADOR, true));
  }

  @Test
  void shouldRefuseAndWriteNothingWhenTheUserIsExterno() {
    assertRefusedAndNothingWritten(
        Usuario.criarExterno("Prestador", Instant.parse("2026-01-01T00:00:00Z")));
  }

  private void assertRefusedAndNothingWritten(final Usuario quemTenta) {
    // Arrange
    when(usuarioRepository.findById(quemTenta.getId())).thenReturn(Optional.of(quemTenta));

    // Act + Assert
    assertThrows(
        NaoAutorizadoException.class,
        () ->
            useCase.execute(
                new EditarInstrucaoSolicitacaoUseCase.Input(
                    UUID.randomUUID(), "Nova", quemTenta.getId())));
    verifyNoInteractions(solicitacaoRepository, atividadeRepository);
  }

  @Test
  void shouldRefuseAnInactiveManager() {
    // Arrange
    final Usuario inativo = usuario(PerfilUsuario.GESTOR, false);
    when(usuarioRepository.findById(inativo.getId())).thenReturn(Optional.of(inativo));

    // Act + Assert
    assertThrows(
        NaoAutorizadoException.class,
        () ->
            useCase.execute(
                new EditarInstrucaoSolicitacaoUseCase.Input(
                    UUID.randomUUID(), "Nova", inativo.getId())));
    verifyNoInteractions(solicitacaoRepository, atividadeRepository);
  }

  @Test
  void shouldFailWhenTheUserDoesNotExist() {
    // Arrange
    final UUID id = UUID.randomUUID();
    when(usuarioRepository.findById(id)).thenReturn(Optional.empty());

    // Act + Assert
    assertThrows(
        RecursoNaoEncontradoException.class,
        () ->
            useCase.execute(
                new EditarInstrucaoSolicitacaoUseCase.Input(UUID.randomUUID(), "Nova", id)));
  }

  @Test
  void shouldFailWhenTheSolicitacaoDoesNotExist() {
    // Arrange
    final Usuario gestor = usuario(PerfilUsuario.GESTOR, true);
    final UUID solicitacaoId = UUID.randomUUID();
    dado(gestor, null);
    when(solicitacaoRepository.findById(solicitacaoId)).thenReturn(Optional.empty());

    // Act + Assert
    assertThrows(
        RecursoNaoEncontradoException.class,
        () ->
            useCase.execute(
                new EditarInstrucaoSolicitacaoUseCase.Input(
                    solicitacaoId, "Nova", gestor.getId())));
    verifyNoInteractions(atividadeRepository);
  }

  @ParameterizedTest
  @EnumSource(
      value = StatusSolicitacao.class,
      names = {"A_FAZER", "EM_VALIDACAO", "CONCLUIDA", "CANCELADA"})
  void shouldRefuseAndWriteNothingOutsideEmAndamento(final StatusSolicitacao status) {
    // Arrange
    final Usuario gestor = usuario(PerfilUsuario.GESTOR, true);
    final Solicitacao sol = umaSolicitacao().noStatus(status).build();
    dado(gestor, sol);

    // Act + Assert
    assertThrows(
        ConflitoException.class,
        () ->
            useCase.execute(
                new EditarInstrucaoSolicitacaoUseCase.Input(sol.getId(), "Nova", gestor.getId())));
    verify(solicitacaoRepository, never()).save(any());
    verifyNoInteractions(atividadeRepository);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void shouldFailAndWriteNothingWhenTheTextIsBlank(final String texto) {
    // Arrange
    final Usuario gestor = usuario(PerfilUsuario.GESTOR, true);
    final Solicitacao sol = umaSolicitacao().emAndamento().comInstrucao("Antiga").build();
    dado(gestor, sol);

    // Act + Assert
    assertThrows(
        ValidationException.class,
        () ->
            useCase.execute(
                new EditarInstrucaoSolicitacaoUseCase.Input(sol.getId(), texto, gestor.getId())));
    verify(solicitacaoRepository, never()).save(any());
    verifyNoInteractions(atividadeRepository);
  }
}
