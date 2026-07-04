package com.rgm.api.core.application.usecases.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Maquina;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.MaquinaRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GerenciarMaquinasUseCaseTest {

  private MaquinaRepository maquinaRepository;
  private UsuarioRepository usuarioRepository;
  private GerenciarMaquinasUseCase useCase;

  @BeforeEach
  void setUp() {
    maquinaRepository = mock(MaquinaRepository.class);
    usuarioRepository = mock(UsuarioRepository.class);
    when(maquinaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    useCase = new GerenciarMaquinasUseCase(maquinaRepository, usuarioRepository);
  }

  private Usuario usuario(final PerfilUsuario perfil) {
    final Instant agora = Instant.now();
    return new Usuario(UUID.randomUUID(), "U", "u@test.com", "hash", perfil, true, agora, agora);
  }

  private Maquina maquina(final String nome, final boolean ativo) {
    final Instant agora = Instant.now();
    return new Maquina(UUID.randomUUID(), nome, ativo, agora, agora);
  }

  @Test
  void adminCriaComSucesso() {
    final Usuario admin = usuario(PerfilUsuario.ADMINISTRADOR);
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(maquinaRepository.existsByNome("VICK")).thenReturn(false);

    final Maquina result =
        useCase.criar(new GerenciarMaquinasUseCase.CriarInput("  VICK  ", admin.getId()));

    assertEquals("VICK", result.getNome());
    assertTrue(result.isAtivo());
  }

  @Test
  void gestorNaoPodeCriar() {
    final Usuario gestor = usuario(PerfilUsuario.GESTOR);
    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));

    assertThrows(
        NaoAutorizadoException.class,
        () -> useCase.criar(new GerenciarMaquinasUseCase.CriarInput("VICK", gestor.getId())));
  }

  @Test
  void naoPermiteNomeDuplicado() {
    final Usuario admin = usuario(PerfilUsuario.ADMINISTRADOR);
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(maquinaRepository.existsByNome("VICK")).thenReturn(true);

    assertThrows(
        ValidationException.class,
        () -> useCase.criar(new GerenciarMaquinasUseCase.CriarInput("VICK", admin.getId())));
  }

  @Test
  void nomeEmBrancoFalha() {
    final Usuario admin = usuario(PerfilUsuario.ADMINISTRADOR);
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

    assertThrows(
        ValidationException.class,
        () -> useCase.criar(new GerenciarMaquinasUseCase.CriarInput("   ", admin.getId())));
  }

  @Test
  void renomeiaComSucesso() {
    final Usuario admin = usuario(PerfilUsuario.ADMINISTRADOR);
    final Maquina existente = maquina("VICK", true);
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(maquinaRepository.findById(existente.getId())).thenReturn(Optional.of(existente));
    when(maquinaRepository.existsByNome("FBO")).thenReturn(false);

    final Maquina result =
        useCase.renomear(
            new GerenciarMaquinasUseCase.RenomearInput(existente.getId(), "FBO", admin.getId()));

    assertEquals("FBO", result.getNome());
  }

  @Test
  void renomearParaMesmoNomeNaoConflita() {
    final Usuario admin = usuario(PerfilUsuario.ADMINISTRADOR);
    final Maquina existente = maquina("VICK", true);
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(maquinaRepository.findById(existente.getId())).thenReturn(Optional.of(existente));

    final Maquina result =
        useCase.renomear(
            new GerenciarMaquinasUseCase.RenomearInput(existente.getId(), "VICK", admin.getId()));

    assertEquals("VICK", result.getNome());
    verify(maquinaRepository, never()).existsByNome(any());
  }

  @Test
  void renomearParaNomeExistenteFalha() {
    final Usuario admin = usuario(PerfilUsuario.ADMINISTRADOR);
    final Maquina existente = maquina("VICK", true);
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(maquinaRepository.findById(existente.getId())).thenReturn(Optional.of(existente));
    when(maquinaRepository.existsByNome("FBO")).thenReturn(true);

    assertThrows(
        ValidationException.class,
        () ->
            useCase.renomear(
                new GerenciarMaquinasUseCase.RenomearInput(
                    existente.getId(), "FBO", admin.getId())));
  }

  @Test
  void renomearMaquinaInexistenteFalha() {
    final Usuario admin = usuario(PerfilUsuario.ADMINISTRADOR);
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(maquinaRepository.findById(any())).thenReturn(Optional.empty());

    assertThrows(
        RecursoNaoEncontradoException.class,
        () ->
            useCase.renomear(
                new GerenciarMaquinasUseCase.RenomearInput(
                    UUID.randomUUID(), "FBO", admin.getId())));
  }

  @Test
  void desativaEAtiva() {
    final Usuario admin = usuario(PerfilUsuario.ADMINISTRADOR);
    final Maquina ativa = maquina("VICK", true);
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(maquinaRepository.findById(ativa.getId())).thenReturn(Optional.of(ativa));

    final Maquina desativada =
        useCase.desativar(
            new GerenciarMaquinasUseCase.DesativarInput(ativa.getId(), admin.getId()));
    assertFalse(desativada.isAtivo());

    final Maquina reativada =
        useCase.ativar(new GerenciarMaquinasUseCase.AtivarInput(ativa.getId(), admin.getId()));
    assertTrue(reativada.isAtivo());
  }

  @Test
  void usuarioInexistenteFalha() {
    when(usuarioRepository.findById(any())).thenReturn(Optional.empty());

    assertThrows(
        RecursoNaoEncontradoException.class,
        () -> useCase.criar(new GerenciarMaquinasUseCase.CriarInput("VICK", UUID.randomUUID())));
  }
}
