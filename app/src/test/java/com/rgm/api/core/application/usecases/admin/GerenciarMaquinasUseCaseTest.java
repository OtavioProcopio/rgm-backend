package com.rgm.api.core.application.usecases.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
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
    useCase = new GerenciarMaquinasUseCase(maquinaRepository, usuarioRepository);
  }

  private Usuario criarAdmin() {
    final Instant agora = Instant.now();
    return new Usuario(
        UUID.randomUUID(),
        "Admin",
        "admin@test.com",
        "hash",
        PerfilUsuario.ADMINISTRADOR,
        true,
        agora,
        agora);
  }

  @Test
  void deveCriarMaquinaComoAdmin() {
    final Usuario admin = criarAdmin();

    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(maquinaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final Maquina resultado =
        useCase.criar(
            new GerenciarMaquinasUseCase.CriarInput(
                "Injetora 03", "INJ-03", "Desc", admin.getId()));

    assertNotNull(resultado);
    assertEquals("Injetora 03", resultado.getNome());
    assertEquals("INJ-03", resultado.getCodigo());
  }

  @Test
  void deveEditarMaquinaComoAdmin() {
    final Usuario admin = criarAdmin();
    final Instant agora = Instant.now();
    final Maquina maquina = Maquina.criar("Old", "OLD-01", "old desc", agora);

    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(maquinaRepository.findById(maquina.getId())).thenReturn(Optional.of(maquina));
    when(maquinaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final Maquina resultado =
        useCase.editar(
            new GerenciarMaquinasUseCase.EditarInput(
                maquina.getId(), "Novo Nome", "NEW-01", "nova desc", admin.getId()));

    assertEquals("Novo Nome", resultado.getNome());
    assertEquals("NEW-01", resultado.getCodigo());
  }

  @Test
  void operadorNaoDeveCriarMaquina() {
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
            useCase.criar(
                new GerenciarMaquinasUseCase.CriarInput("M", "C", "D", operador.getId())));
  }

  @Test
  void gestorNaoDeveCriarMaquina() {
    final Instant agora = Instant.now();
    final Usuario gestor =
        new Usuario(
            UUID.randomUUID(),
            "Gestor",
            "g@test.com",
            "hash",
            PerfilUsuario.GESTOR,
            true,
            agora,
            agora);

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));

    assertThrows(
        NaoAutorizadoException.class,
        () ->
            useCase.criar(new GerenciarMaquinasUseCase.CriarInput("M", "C", "D", gestor.getId())));
  }

  @Test
  void deveFalharAoEditarMaquinaNaoEncontrada() {
    final Usuario admin = criarAdmin();

    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(maquinaRepository.findById(any())).thenReturn(Optional.empty());

    assertThrows(
        RecursoNaoEncontradoException.class,
        () ->
            useCase.editar(
                new GerenciarMaquinasUseCase.EditarInput(
                    UUID.randomUUID(), "N", "C", "D", admin.getId())));
  }
}
