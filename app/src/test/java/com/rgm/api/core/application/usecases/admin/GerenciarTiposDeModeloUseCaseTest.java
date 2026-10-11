package com.rgm.api.core.application.usecases.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.TipoDeModelo;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.TipoDeModeloRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GerenciarTiposDeModeloUseCaseTest {

  private TipoDeModeloRepository tipoDeModeloRepository;
  private UsuarioRepository usuarioRepository;
  private ModeloRepository modeloRepository;
  private GerenciarTiposDeModeloUseCase useCase;

  @BeforeEach
  void setUp() {
    tipoDeModeloRepository = mock(TipoDeModeloRepository.class);
    usuarioRepository = mock(UsuarioRepository.class);
    modeloRepository = mock(ModeloRepository.class);
    when(tipoDeModeloRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    useCase =
        new GerenciarTiposDeModeloUseCase(
            tipoDeModeloRepository, usuarioRepository, modeloRepository);
  }

  private Usuario usuario(final PerfilUsuario perfil) {
    final Instant agora = Instant.now();
    return new Usuario(UUID.randomUUID(), "U", "u@test.com", "hash", perfil, true, agora, agora);
  }

  private TipoDeModelo tipoDeModelo(final String nome, final boolean ativo) {
    final Instant agora = Instant.now();
    return new TipoDeModelo(UUID.randomUUID(), nome, ativo, agora, agora);
  }

  @Test
  void adminCriaComSucesso() {
    final Usuario admin = usuario(PerfilUsuario.ADMINISTRADOR);
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(tipoDeModeloRepository.existsByNome("VICK")).thenReturn(false);

    final TipoDeModelo result =
        useCase.criar(new GerenciarTiposDeModeloUseCase.CriarInput("  VICK  ", admin.getId()));

    assertEquals("VICK", result.getNome());
    assertTrue(result.isAtivo());
  }

  @Test
  void gestorNaoPodeCriar() {
    final Usuario gestor = usuario(PerfilUsuario.GESTOR);
    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));

    assertThrows(
        NaoAutorizadoException.class,
        () -> useCase.criar(new GerenciarTiposDeModeloUseCase.CriarInput("VICK", gestor.getId())));
  }

  @Test
  void naoPermiteNomeDuplicado() {
    final Usuario admin = usuario(PerfilUsuario.ADMINISTRADOR);
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(tipoDeModeloRepository.existsByNome("VICK")).thenReturn(true);

    assertThrows(
        ValidationException.class,
        () -> useCase.criar(new GerenciarTiposDeModeloUseCase.CriarInput("VICK", admin.getId())));
  }

  @Test
  void nomeEmBrancoFalha() {
    final Usuario admin = usuario(PerfilUsuario.ADMINISTRADOR);
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

    assertThrows(
        ValidationException.class,
        () -> useCase.criar(new GerenciarTiposDeModeloUseCase.CriarInput("   ", admin.getId())));
  }

  @Test
  void renomeiaComSucesso() {
    final Usuario admin = usuario(PerfilUsuario.ADMINISTRADOR);
    final TipoDeModelo existente = tipoDeModelo("VICK", true);
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(tipoDeModeloRepository.findById(existente.getId())).thenReturn(Optional.of(existente));
    when(tipoDeModeloRepository.existsByNome("FBO")).thenReturn(false);

    final TipoDeModelo result =
        useCase.renomear(
            new GerenciarTiposDeModeloUseCase.RenomearInput(
                existente.getId(), "FBO", admin.getId()));

    assertEquals("FBO", result.getNome());
  }

  @Test
  void renomearParaMesmoNomeNaoConflita() {
    final Usuario admin = usuario(PerfilUsuario.ADMINISTRADOR);
    final TipoDeModelo existente = tipoDeModelo("VICK", true);
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(tipoDeModeloRepository.findById(existente.getId())).thenReturn(Optional.of(existente));

    final TipoDeModelo result =
        useCase.renomear(
            new GerenciarTiposDeModeloUseCase.RenomearInput(
                existente.getId(), "VICK", admin.getId()));

    assertEquals("VICK", result.getNome());
    verify(tipoDeModeloRepository, never()).existsByNome(any());
  }

  @Test
  void renomearParaNomeExistenteFalha() {
    final Usuario admin = usuario(PerfilUsuario.ADMINISTRADOR);
    final TipoDeModelo existente = tipoDeModelo("VICK", true);
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(tipoDeModeloRepository.findById(existente.getId())).thenReturn(Optional.of(existente));
    when(tipoDeModeloRepository.existsByNome("FBO")).thenReturn(true);

    assertThrows(
        ValidationException.class,
        () ->
            useCase.renomear(
                new GerenciarTiposDeModeloUseCase.RenomearInput(
                    existente.getId(), "FBO", admin.getId())));
  }

  @Test
  void renomearTipoDeModeloInexistenteFalha() {
    final Usuario admin = usuario(PerfilUsuario.ADMINISTRADOR);
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(tipoDeModeloRepository.findById(any())).thenReturn(Optional.empty());

    assertThrows(
        RecursoNaoEncontradoException.class,
        () ->
            useCase.renomear(
                new GerenciarTiposDeModeloUseCase.RenomearInput(
                    UUID.randomUUID(), "FBO", admin.getId())));
  }

  @Test
  void desativaEAtiva() {
    final Usuario admin = usuario(PerfilUsuario.ADMINISTRADOR);
    final TipoDeModelo ativa = tipoDeModelo("VICK", true);
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(tipoDeModeloRepository.findById(ativa.getId())).thenReturn(Optional.of(ativa));

    final TipoDeModelo desativada =
        useCase.desativar(
            new GerenciarTiposDeModeloUseCase.DesativarInput(ativa.getId(), admin.getId()));
    assertFalse(desativada.isAtivo());

    final TipoDeModelo reativada =
        useCase.ativar(new GerenciarTiposDeModeloUseCase.AtivarInput(ativa.getId(), admin.getId()));
    assertTrue(reativada.isAtivo());
  }

  @Test
  void usuarioInexistenteFalha() {
    when(usuarioRepository.findById(any())).thenReturn(Optional.empty());

    assertThrows(
        RecursoNaoEncontradoException.class,
        () ->
            useCase.criar(new GerenciarTiposDeModeloUseCase.CriarInput("VICK", UUID.randomUUID())));
  }

  @Test
  void shouldMoveModelosToTheNewNameWhenRenaming() {
    // Arrange
    final Usuario admin = usuario(PerfilUsuario.ADMINISTRADOR);
    final TipoDeModelo existente = tipoDeModelo("ANTIGO", true);
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(tipoDeModeloRepository.findById(existente.getId())).thenReturn(Optional.of(existente));
    when(tipoDeModeloRepository.existsByNome("NOVO")).thenReturn(false);

    // Act
    useCase.renomear(
        new GerenciarTiposDeModeloUseCase.RenomearInput(existente.getId(), "NOVO", admin.getId()));

    // Assert
    verify(modeloRepository, times(1)).trocarTipo("ANTIGO", "NOVO");
  }

  @Test
  void shouldNotTouchModelosWhenRenamingToTheSameName() {
    // Arrange
    final Usuario admin = usuario(PerfilUsuario.ADMINISTRADOR);
    final TipoDeModelo existente = tipoDeModelo("IGUAL", true);
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(tipoDeModeloRepository.findById(existente.getId())).thenReturn(Optional.of(existente));

    // Act
    useCase.renomear(
        new GerenciarTiposDeModeloUseCase.RenomearInput(existente.getId(), "IGUAL", admin.getId()));

    // Assert
    verify(modeloRepository, never()).trocarTipo(any(), any());
  }
}
