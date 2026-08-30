package com.rgm.api.core.application.usecases.modelo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.EventoModeloRepository;
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
  private UsuarioRepository usuarioRepository;
  private MaquinaRepository maquinaRepository;
  private EventoModeloRepository eventoModeloRepository;
  private GerenciarModelosUseCase useCase;

  @BeforeEach
  void setUp() {
    modeloRepository = mock(ModeloRepository.class);
    usuarioRepository = mock(UsuarioRepository.class);
    maquinaRepository = mock(MaquinaRepository.class);
    eventoModeloRepository = mock(EventoModeloRepository.class);
    when(maquinaRepository.existsByNomeAndAtivoTrue(any())).thenReturn(true);
    useCase =
        new GerenciarModelosUseCase(
            modeloRepository, usuarioRepository, maquinaRepository, eventoModeloRepository);
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

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(modeloRepository.countByMaquinaAndCodigo("FBOX", "MOD-01")).thenReturn(0);
    when(modeloRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final Modelo resultado =
        useCase.criar(
            new GerenciarModelosUseCase.CriarInput(
                "MOD-01", "Descricao", null, "FBOX", null, gestor.getId()));

    assertNotNull(resultado);
    assertEquals("MOD-01", resultado.getCodigo());
    assertEquals("FBOX", resultado.getMaquina());
    assertTrue(resultado.isAtivo());

    final var eventoCaptor =
        org.mockito.ArgumentCaptor.forClass(
            com.rgm.api.core.domain.model.aggregates.EventoModelo.class);
    verify(eventoModeloRepository).save(eventoCaptor.capture());
    final var evento = eventoCaptor.getValue();
    assertEquals(com.rgm.api.core.domain.model.enums.TipoEventoModelo.CADASTRO, evento.getTipo());
    assertEquals(resultado.getId(), evento.getModeloId());
    assertNull(evento.getSolicitacaoRelacionadaId(), "cadastro direto nao referencia solicitacao");
  }

  @Test
  void deveCriarModeloComReferenciaASolicitacaoDeOrigem() {
    final Usuario gestor = criarGestor();
    final UUID solicitacaoId = UUID.randomUUID();

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(modeloRepository.countByMaquinaAndCodigo("FBOX", "MOD-02")).thenReturn(0);
    when(modeloRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    useCase.criar(
        new GerenciarModelosUseCase.CriarInput(
            "MOD-02", "Descricao", null, "FBOX", null, gestor.getId(), solicitacaoId));

    final var eventoCaptor =
        org.mockito.ArgumentCaptor.forClass(
            com.rgm.api.core.domain.model.aggregates.EventoModelo.class);
    verify(eventoModeloRepository).save(eventoCaptor.capture());
    assertEquals(solicitacaoId, eventoCaptor.getValue().getSolicitacaoRelacionadaId());
  }

  @Test
  void deveFalharSeMaquinaInexistenteOuInativa() {
    final Usuario gestor = criarGestor();
    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(maquinaRepository.existsByNomeAndAtivoTrue("INEXISTENTE")).thenReturn(false);

    assertThrows(
        ValidationException.class,
        () ->
            useCase.criar(
                new GerenciarModelosUseCase.CriarInput(
                    "MOD-01", "Descricao", null, "INEXISTENTE", null, gestor.getId())));
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
                    "M", "D", null, "FBOX", null, operador.getId())));
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
            true,
            "FBOX",
            null,
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

  @Test
  void deveReativarModeloComSucesso() {
    final Usuario gestor = criarGestor();
    final Instant agora = Instant.now();
    final Modelo modeloInativo =
        new Modelo(
            UUID.randomUUID(),
            "MOD-01",
            1,
            "Desc",
            null,
            null,
            null,
            false,
            "FBOX",
            null,
            false,
            agora,
            agora);

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(modeloRepository.findById(modeloInativo.getId())).thenReturn(Optional.of(modeloInativo));
    when(modeloRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final Modelo resultado =
        useCase.reativar(
            new GerenciarModelosUseCase.ReativarInput(modeloInativo.getId(), gestor.getId()));

    assertNotNull(resultado);
    assertTrue(resultado.isAtivo());
  }

  @Test
  void deveFalharSeOperadorTentaReativar() {
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
            useCase.reativar(
                new GerenciarModelosUseCase.ReativarInput(UUID.randomUUID(), operador.getId())));
  }

  @Test
  void deveEditarModeloComSucesso() {
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
            true,
            "FBOX",
            null,
            false,
            agora,
            agora);

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(modeloRepository.findById(modelo.getId())).thenReturn(Optional.of(modelo));
    when(modeloRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final Modelo resultado =
        useCase.editar(
            new GerenciarModelosUseCase.EditarInput(
                modelo.getId(), "MOD-EDITADO", "Desc Editada", "Obs", "MAQ", null, gestor.getId()));

    assertNotNull(resultado);
    assertEquals("MOD-EDITADO", resultado.getCodigo());
    assertEquals("Desc Editada", resultado.getDescricao());
    assertEquals("MAQ", resultado.getMaquina());
  }

  @Test
  void deveFalharEditarSeModeloNaoExistir() {
    final Usuario gestor = criarGestor();
    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(modeloRepository.findById(any())).thenReturn(Optional.empty());

    assertThrows(
        RecursoNaoEncontradoException.class,
        () ->
            useCase.editar(
                new GerenciarModelosUseCase.EditarInput(
                    UUID.randomUUID(), "MOD", "Desc", "Obs", "MAQ", null, gestor.getId())));
  }

  @Test
  void deveFalharDesativarSeModeloNaoExistir() {
    final Usuario gestor = criarGestor();
    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(modeloRepository.findById(any())).thenReturn(Optional.empty());

    assertThrows(
        RecursoNaoEncontradoException.class,
        () ->
            useCase.desativar(
                new GerenciarModelosUseCase.DesativarInput(UUID.randomUUID(), gestor.getId())));
  }

  @Test
  void deveFalharSeUsuarioNaoExistir() {
    when(usuarioRepository.findById(any())).thenReturn(Optional.empty());

    assertThrows(
        RecursoNaoEncontradoException.class,
        () ->
            useCase.reativar(
                new GerenciarModelosUseCase.ReativarInput(UUID.randomUUID(), UUID.randomUUID())));
  }
}
