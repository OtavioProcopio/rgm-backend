package com.rgm.api.core.application.usecases.modelo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Maquina;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.MaquinaRepository;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GerenciarModelosUseCaseTest {

  private ModeloRepository modeloRepository;
  private MaquinaRepository maquinaRepository;
  private UsuarioRepository usuarioRepository;
  private GerenciarModelosUseCase useCase;

  @BeforeEach
  void setUp() {
    modeloRepository = mock(ModeloRepository.class);
    maquinaRepository = mock(MaquinaRepository.class);
    usuarioRepository = mock(UsuarioRepository.class);
    useCase = new GerenciarModelosUseCase(modeloRepository, maquinaRepository, usuarioRepository);
  }

  private Usuario criarGestor() {
    final Instant agora = Instant.now();
    return new Usuario(
        UUID.randomUUID(),
        "Gestor",
        "g@test.com",
        "hash",
        PerfilUsuario.GESTOR,
        true,
        agora,
        agora);
  }

  @Test
  void deveCriarModeloComSucesso() {
    final Usuario gestor = criarGestor();
    final Instant agora = Instant.now();
    final Maquina maquina =
        new Maquina(UUID.randomUUID(), "Maq 1", "MAQ-01", "desc", true, agora, agora);

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(maquinaRepository.findById(maquina.getId())).thenReturn(Optional.of(maquina));
    when(modeloRepository.countByMaquinaIdAndCodigo(maquina.getId(), "MOD-01")).thenReturn(0);
    when(modeloRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final Modelo resultado =
        useCase.criar(
            new GerenciarModelosUseCase.CriarInput(
                "MOD-01", "Descricao", null, maquina.getId(), gestor.getId()));

    assertNotNull(resultado);
    assertEquals("MOD-01", resultado.getCodigo());
    assertTrue(resultado.isAtivo());
  }

  @Test
  void deveFalharComMaquinaInativa() {
    final Usuario gestor = criarGestor();
    final Instant agora = Instant.now();
    final Maquina inativa =
        new Maquina(UUID.randomUUID(), "Maq", "MAQ-01", null, false, agora, agora);

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(maquinaRepository.findById(inativa.getId())).thenReturn(Optional.of(inativa));

    assertThrows(
        ValidationException.class,
        () ->
            useCase.criar(
                new GerenciarModelosUseCase.CriarInput(
                    "M", "D", null, inativa.getId(), gestor.getId())));
  }

  @Test
  void deveFalharSeOperadorTentaCriar() {
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
                new GerenciarModelosUseCase.CriarInput(
                    "M", "D", null, UUID.randomUUID(), operador.getId())));
  }

  @Test
  void deveDesativarModelo() {
    final Usuario gestor = criarGestor();
    final Instant agora = Instant.now();
    final Modelo modelo =
        new Modelo(
            UUID.randomUUID(),
            "MOD-01",
            1,
            "Desc",
            null,
            null,
            null,
            null,
            null,
            true,
            UUID.randomUUID(),
            false,
            agora,
            agora);

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(modeloRepository.findById(modelo.getId())).thenReturn(Optional.of(modelo));
    when(modeloRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final Modelo resultado =
        useCase.desativar(
            new GerenciarModelosUseCase.DesativarInput(modelo.getId(), gestor.getId()));

    assertFalse(resultado.isAtivo());
  }
}
