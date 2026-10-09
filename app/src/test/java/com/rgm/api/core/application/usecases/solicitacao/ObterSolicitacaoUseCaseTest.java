package com.rgm.api.core.application.usecases.solicitacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.entities.SolicitacaoAtribuicao;
import com.rgm.api.core.domain.model.enums.AcaoSolicitacao;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ObterSolicitacaoUseCaseTest {

  @Mock private SolicitacaoRepository solicitacaoRepository;
  @Mock private SolicitacaoAtribuicaoRepository atribuicaoRepository;
  @Mock private UsuarioRepository usuarioRepository;
  @InjectMocks private ObterSolicitacaoUseCase useCase;

  @Test
  void execute_retornaSolicitacaoComResponsaveis() {
    final UUID solId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final Solicitacao sol =
        Solicitacao.abrir(
            "T", "D", TipoSolicitacao.REPARO, UUID.randomUUID(), userId, Instant.now());

    final SolicitacaoAtribuicao atrib =
        new SolicitacaoAtribuicao(
            UUID.randomUUID(), solId, userId, UUID.randomUUID(), Instant.now(), null);

    when(solicitacaoRepository.findById(solId)).thenReturn(Optional.of(sol));
    when(atribuicaoRepository.findBySolicitacaoId(solId)).thenReturn(List.of(atrib));
    when(usuarioRepository.findById(userId)).thenReturn(Optional.of(operador(userId)));

    final var output = useCase.execute(new ObterSolicitacaoUseCase.Input(solId, userId));

    assertThat(output.solicitacao()).isEqualTo(sol);
    assertThat(output.responsavelIds()).containsExactly(userId);
  }

  @Test
  void execute_filtraRemovidoEm() {
    final UUID solId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final Solicitacao sol =
        Solicitacao.abrir(
            "T", "D", TipoSolicitacao.REPARO, UUID.randomUUID(), userId, Instant.now());

    final SolicitacaoAtribuicao removida =
        new SolicitacaoAtribuicao(
            UUID.randomUUID(), solId, userId, UUID.randomUUID(), Instant.now(), Instant.now());

    when(solicitacaoRepository.findById(solId)).thenReturn(Optional.of(sol));
    when(atribuicaoRepository.findBySolicitacaoId(solId)).thenReturn(List.of(removida));
    when(usuarioRepository.findById(userId)).thenReturn(Optional.of(operador(userId)));

    final var output = useCase.execute(new ObterSolicitacaoUseCase.Input(solId, userId));

    assertThat(output.responsavelIds()).isEmpty();
  }

  @Test
  void execute_lancaExcecaoSeNaoEncontrado() {
    final UUID solId = UUID.randomUUID();
    when(solicitacaoRepository.findById(solId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> useCase.execute(new ObterSolicitacaoUseCase.Input(solId, UUID.randomUUID())))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }

  @Test
  void listarResponsaveisBatch_vazio() {
    final var result = useCase.listarResponsaveisBatch(List.of());
    assertThat(result).isEmpty();
  }

  @Test
  void listarResponsaveisBatch_agrupaPorSolicitacao() {
    final UUID solId1 = UUID.randomUUID();
    final UUID solId2 = UUID.randomUUID();
    final UUID user1 = UUID.randomUUID();
    final UUID user2 = UUID.randomUUID();

    final var atrib1 =
        new SolicitacaoAtribuicao(
            UUID.randomUUID(), solId1, user1, UUID.randomUUID(), Instant.now(), null);
    final var atrib2 =
        new SolicitacaoAtribuicao(
            UUID.randomUUID(), solId2, user2, UUID.randomUUID(), Instant.now(), null);
    final var removida =
        new SolicitacaoAtribuicao(
            UUID.randomUUID(), solId1, user2, UUID.randomUUID(), Instant.now(), Instant.now());

    when(atribuicaoRepository.findBySolicitacaoIdIn(List.of(solId1, solId2)))
        .thenReturn(List.of(atrib1, atrib2, removida));

    final var result = useCase.listarResponsaveisBatch(List.of(solId1, solId2));

    assertThat(result.get(solId1)).containsExactly(user1);
    assertThat(result.get(solId2)).containsExactly(user2);
  }

  private static Usuario operador(final UUID id) {
    final Instant agora = Instant.now();
    return new Usuario(
        id, "Operador", "operador@rgm.test", "hash", PerfilUsuario.OPERADOR, true, agora, agora);
  }

  @Test
  void shouldReturnAllowedActionsWhenAuthorReadsUnassignedSolicitacao() {
    // Arrange
    final UUID autorId = UUID.randomUUID();
    final Solicitacao sol =
        Solicitacao.abrir(
            "T", "D", TipoSolicitacao.REPARO, UUID.randomUUID(), autorId, Instant.now());
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(autorId)).thenReturn(Optional.of(operador(autorId)));
    when(atribuicaoRepository.findBySolicitacaoId(sol.getId())).thenReturn(List.of());

    // Act
    final var output = useCase.execute(new ObterSolicitacaoUseCase.Input(sol.getId(), autorId));

    // Assert
    assertThat(output)
        .isEqualTo(
            new ObterSolicitacaoUseCase.Output(
                sol,
                List.of(),
                List.of(),
                null,
                EnumSet.of(
                    AcaoSolicitacao.CANCELAR,
                    AcaoSolicitacao.EDITAR,
                    AcaoSolicitacao.COMENTAR,
                    AcaoSolicitacao.ANEXAR_EVIDENCIA)));
    verify(solicitacaoRepository, times(1)).findById(sol.getId());
    verify(usuarioRepository, times(1)).findById(autorId);
    verify(atribuicaoRepository, times(1)).findBySolicitacaoId(sol.getId());
    verify(usuarioRepository, times(1)).findAllByIdIn(List.of(autorId));
    verifyNoMoreInteractions(solicitacaoRepository, usuarioRepository, atribuicaoRepository);
  }

  @Test
  void shouldFailWhenRequestingUserDoesNotExist() {
    // Arrange
    final UUID usuarioId = UUID.randomUUID();
    final Solicitacao sol =
        Solicitacao.abrir(
            "T", "D", TipoSolicitacao.REPARO, UUID.randomUUID(), usuarioId, Instant.now());
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

    // Act
    final var erro =
        org.junit.jupiter.api.Assertions.assertThrows(
            RecursoNaoEncontradoException.class,
            () -> useCase.execute(new ObterSolicitacaoUseCase.Input(sol.getId(), usuarioId)));

    // Assert
    assertThat(erro.getMessage()).isEqualTo("Usuario nao encontrado");
    verify(solicitacaoRepository, times(1)).findById(sol.getId());
    verify(usuarioRepository, times(1)).findById(usuarioId);
    verifyNoMoreInteractions(solicitacaoRepository, usuarioRepository, atribuicaoRepository);
  }

  private static Usuario usuario(final UUID id, final PerfilUsuario perfil) {
    final Instant agora = Instant.now();
    return new Usuario(id, "Usuario", "usuario@rgm.test", "hash", perfil, true, agora, agora);
  }

  @Test
  void shouldDenyWhenOperatorNeitherOpenedNorIsAssigned() {
    // Arrange
    final UUID operadorId = UUID.randomUUID();
    final Solicitacao sol =
        Solicitacao.abrir(
            "T", "D", TipoSolicitacao.REPARO, UUID.randomUUID(), UUID.randomUUID(), Instant.now());
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(operadorId)).thenReturn(Optional.of(operador(operadorId)));
    when(atribuicaoRepository.findBySolicitacaoId(sol.getId())).thenReturn(List.of());

    // Act
    final var erro =
        org.junit.jupiter.api.Assertions.assertThrows(
            NaoAutorizadoException.class,
            () -> useCase.execute(new ObterSolicitacaoUseCase.Input(sol.getId(), operadorId)));

    // Assert
    assertThat(erro.getMessage()).isEqualTo("Usuario nao tem acesso a esta solicitacao");
    verify(solicitacaoRepository, times(1)).findById(sol.getId());
    verify(usuarioRepository, times(1)).findById(operadorId);
    verify(atribuicaoRepository, times(1)).findBySolicitacaoId(sol.getId());
    verifyNoMoreInteractions(solicitacaoRepository, usuarioRepository, atribuicaoRepository);
  }

  @Test
  void shouldReturnSolicitacaoWhenOperatorIsAssignedButDidNotOpenIt() {
    // Arrange
    final UUID operadorId = UUID.randomUUID();
    final Solicitacao sol =
        Solicitacao.abrir(
            "T", "D", TipoSolicitacao.REPARO, UUID.randomUUID(), UUID.randomUUID(), Instant.now());
    final SolicitacaoAtribuicao atribuicao =
        new SolicitacaoAtribuicao(
            UUID.randomUUID(), sol.getId(), operadorId, UUID.randomUUID(), Instant.now(), null);
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(operadorId)).thenReturn(Optional.of(operador(operadorId)));
    when(atribuicaoRepository.findBySolicitacaoId(sol.getId())).thenReturn(List.of(atribuicao));

    // Act
    final var output = useCase.execute(new ObterSolicitacaoUseCase.Input(sol.getId(), operadorId));

    // Assert
    assertThat(output.solicitacao()).isSameAs(sol);
    verify(solicitacaoRepository, times(1)).findById(sol.getId());
    verify(usuarioRepository, times(1)).findById(operadorId);
    verify(usuarioRepository, times(1))
        .findAllByIdIn(List.of(operadorId, sol.getAbertaPorUsuarioId()));
    verify(atribuicaoRepository, times(1)).findBySolicitacaoId(sol.getId());
    verifyNoMoreInteractions(solicitacaoRepository, usuarioRepository, atribuicaoRepository);
  }

  @Test
  void shouldDenyWhenOperatorWasRemovedFromTheSolicitacao() {
    // Arrange
    final UUID operadorId = UUID.randomUUID();
    final Solicitacao sol =
        Solicitacao.abrir(
            "T", "D", TipoSolicitacao.REPARO, UUID.randomUUID(), UUID.randomUUID(), Instant.now());
    final SolicitacaoAtribuicao removida =
        new SolicitacaoAtribuicao(
            UUID.randomUUID(),
            sol.getId(),
            operadorId,
            UUID.randomUUID(),
            Instant.now(),
            Instant.now());
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(operadorId)).thenReturn(Optional.of(operador(operadorId)));
    when(atribuicaoRepository.findBySolicitacaoId(sol.getId())).thenReturn(List.of(removida));

    // Act
    final var erro =
        org.junit.jupiter.api.Assertions.assertThrows(
            NaoAutorizadoException.class,
            () -> useCase.execute(new ObterSolicitacaoUseCase.Input(sol.getId(), operadorId)));

    // Assert
    assertThat(erro.getMessage()).isEqualTo("Usuario nao tem acesso a esta solicitacao");
    verify(solicitacaoRepository, times(1)).findById(sol.getId());
    verify(usuarioRepository, times(1)).findById(operadorId);
    verify(atribuicaoRepository, times(1)).findBySolicitacaoId(sol.getId());
    verifyNoMoreInteractions(solicitacaoRepository, usuarioRepository, atribuicaoRepository);
  }

  @Test
  void shouldReturnSolicitacaoWhenManagerReadsSolicitacaoOfSomeoneElse() {
    // Arrange
    final UUID gestorId = UUID.randomUUID();
    final Solicitacao sol =
        Solicitacao.abrir(
            "T", "D", TipoSolicitacao.REPARO, UUID.randomUUID(), UUID.randomUUID(), Instant.now());
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(gestorId))
        .thenReturn(Optional.of(usuario(gestorId, PerfilUsuario.GESTOR)));
    when(atribuicaoRepository.findBySolicitacaoId(sol.getId())).thenReturn(List.of());

    // Act
    final var output = useCase.execute(new ObterSolicitacaoUseCase.Input(sol.getId(), gestorId));

    // Assert
    assertThat(output.solicitacao()).isSameAs(sol);
    verify(solicitacaoRepository, times(1)).findById(sol.getId());
    verify(usuarioRepository, times(1)).findById(gestorId);
    verify(usuarioRepository, times(1)).findAllByIdIn(List.of(sol.getAbertaPorUsuarioId()));
    verify(atribuicaoRepository, times(1)).findBySolicitacaoId(sol.getId());
    verifyNoMoreInteractions(solicitacaoRepository, usuarioRepository, atribuicaoRepository);
  }

  private static Usuario nomeado(final UUID id, final String nome, final boolean ativo) {
    final Instant agora = Instant.now();
    return new Usuario(
        id, nome, nome + "@rgm.test", "hash", PerfilUsuario.OPERADOR, ativo, agora, agora);
  }

  private static SolicitacaoAtribuicao atribuicao(final UUID solicitacaoId, final UUID usuarioId) {
    return new SolicitacaoAtribuicao(
        UUID.randomUUID(), solicitacaoId, usuarioId, UUID.randomUUID(), Instant.now(), null);
  }

  @Test
  void shouldReturnNamesInResponsavelIdsOrderWhenSolicitacaoHasAssigneesAndOpener() {
    // Arrange
    final UUID gestorId = UUID.randomUUID();
    final UUID autorId = UUID.randomUUID();
    final UUID brunoId = UUID.randomUUID();
    final UUID carlaId = UUID.randomUUID();
    final Solicitacao sol =
        Solicitacao.abrir(
            "T", "D", TipoSolicitacao.REPARO, UUID.randomUUID(), autorId, Instant.now());
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(gestorId))
        .thenReturn(Optional.of(usuario(gestorId, PerfilUsuario.GESTOR)));
    when(atribuicaoRepository.findBySolicitacaoId(sol.getId()))
        .thenReturn(List.of(atribuicao(sol.getId(), brunoId), atribuicao(sol.getId(), carlaId)));
    when(usuarioRepository.findAllByIdIn(List.of(brunoId, carlaId, autorId)))
        .thenReturn(
            List.of(
                nomeado(autorId, "Ana", true),
                nomeado(carlaId, "Carla", true),
                nomeado(brunoId, "Bruno", true)));

    // Act
    final var output = useCase.execute(new ObterSolicitacaoUseCase.Input(sol.getId(), gestorId));

    // Assert
    assertThat(output.responsavelIds()).containsExactly(brunoId, carlaId);
    assertThat(output.responsaveis())
        .containsExactly(
            new ObterSolicitacaoUseCase.ResponsavelNome(brunoId, "Bruno"),
            new ObterSolicitacaoUseCase.ResponsavelNome(carlaId, "Carla"));
    assertThat(output.abertaPorNome()).isEqualTo("Ana");
    verify(usuarioRepository, times(1)).findAllByIdIn(List.of(brunoId, carlaId, autorId));
  }

  @Test
  void shouldKeepNameWhenAssigneeIsInactive() {
    // Arrange
    final UUID gestorId = UUID.randomUUID();
    final UUID inativoId = UUID.randomUUID();
    final Solicitacao sol =
        Solicitacao.abrir(
            "T", "D", TipoSolicitacao.REPARO, UUID.randomUUID(), UUID.randomUUID(), Instant.now());
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(gestorId))
        .thenReturn(Optional.of(usuario(gestorId, PerfilUsuario.GESTOR)));
    when(atribuicaoRepository.findBySolicitacaoId(sol.getId()))
        .thenReturn(List.of(atribuicao(sol.getId(), inativoId)));
    when(usuarioRepository.findAllByIdIn(List.of(inativoId, sol.getAbertaPorUsuarioId())))
        .thenReturn(List.of(nomeado(inativoId, "Diego", false)));

    // Act
    final var output = useCase.execute(new ObterSolicitacaoUseCase.Input(sol.getId(), gestorId));

    // Assert
    assertThat(output.responsaveis())
        .containsExactly(new ObterSolicitacaoUseCase.ResponsavelNome(inativoId, "Diego"));
  }

  @Test
  void shouldReturnNullNameAndKeepOrderWhenAssigneeAndOpenerAreNotFound() {
    // Arrange
    final UUID gestorId = UUID.randomUUID();
    final UUID orfaoId = UUID.randomUUID();
    final UUID carlaId = UUID.randomUUID();
    final Solicitacao sol =
        Solicitacao.abrir(
            "T", "D", TipoSolicitacao.REPARO, UUID.randomUUID(), UUID.randomUUID(), Instant.now());
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(gestorId))
        .thenReturn(Optional.of(usuario(gestorId, PerfilUsuario.GESTOR)));
    when(atribuicaoRepository.findBySolicitacaoId(sol.getId()))
        .thenReturn(List.of(atribuicao(sol.getId(), orfaoId), atribuicao(sol.getId(), carlaId)));
    when(usuarioRepository.findAllByIdIn(List.of(orfaoId, carlaId, sol.getAbertaPorUsuarioId())))
        .thenReturn(List.of(nomeado(carlaId, "Carla", true)));

    // Act
    final var output = useCase.execute(new ObterSolicitacaoUseCase.Input(sol.getId(), gestorId));

    // Assert
    assertThat(output.responsavelIds()).containsExactly(orfaoId, carlaId);
    assertThat(output.responsaveis())
        .containsExactly(
            new ObterSolicitacaoUseCase.ResponsavelNome(orfaoId, null),
            new ObterSolicitacaoUseCase.ResponsavelNome(carlaId, "Carla"));
    assertThat(output.abertaPorNome()).isNull();
  }

  @Test
  void shouldReturnEmptyResponsaveisWhenSolicitacaoHasNoAssignee() {
    // Arrange
    final UUID gestorId = UUID.randomUUID();
    final UUID autorId = UUID.randomUUID();
    final Solicitacao sol =
        Solicitacao.abrir(
            "T", "D", TipoSolicitacao.REPARO, UUID.randomUUID(), autorId, Instant.now());
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(gestorId))
        .thenReturn(Optional.of(usuario(gestorId, PerfilUsuario.GESTOR)));
    when(atribuicaoRepository.findBySolicitacaoId(sol.getId())).thenReturn(List.of());
    when(usuarioRepository.findAllByIdIn(List.of(autorId)))
        .thenReturn(List.of(nomeado(autorId, "Ana", true)));

    // Act
    final var output = useCase.execute(new ObterSolicitacaoUseCase.Input(sol.getId(), gestorId));

    // Assert
    assertThat(output.responsaveis()).isEmpty();
    assertThat(output.abertaPorNome()).isEqualTo("Ana");
  }

  @Test
  void shouldMapIdsToNamesInOneQueryWhenResolvingNames() {
    // Arrange
    final UUID anaId = UUID.randomUUID();
    final UUID brunoId = UUID.randomUUID();
    final UUID desconhecidoId = UUID.randomUUID();
    when(usuarioRepository.findAllByIdIn(List.of(anaId, brunoId, desconhecidoId)))
        .thenReturn(List.of(nomeado(anaId, "Ana", true), nomeado(brunoId, "Bruno", false)));

    // Act
    final var nomes =
        useCase.resolverNomes(java.util.Arrays.asList(anaId, brunoId, anaId, null, desconhecidoId));

    // Assert
    assertThat(nomes).containsOnlyKeys(anaId, brunoId).containsEntry(anaId, "Ana");
    assertThat(nomes).containsEntry(brunoId, "Bruno");
    verify(usuarioRepository, times(1)).findAllByIdIn(List.of(anaId, brunoId, desconhecidoId));
    verifyNoMoreInteractions(usuarioRepository);
  }

  @Test
  void shouldNotQueryWhenResolvingNamesForEmptyOrNullOnlyIds() {
    // Arrange
    final java.util.List<UUID> soNulos = java.util.Collections.singletonList(null);

    // Act
    final var vazio = useCase.resolverNomes(List.of());
    final var nulos = useCase.resolverNomes(soNulos);

    // Assert
    assertThat(vazio).isEmpty();
    assertThat(nulos).isEmpty();
    verifyNoMoreInteractions(usuarioRepository);
  }
}
