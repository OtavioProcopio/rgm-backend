package com.rgm.api.core.application.usecases.modelo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.exceptions.ConflitoException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.EventoModeloRepository;
import com.rgm.api.core.domain.ports.repositories.MaquinaRepository;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.TipoDeModeloRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class GerenciarModelosUseCaseTest {

  private ModeloRepository modeloRepository;
  private UsuarioRepository usuarioRepository;
  private MaquinaRepository maquinaRepository;
  private TipoDeModeloRepository tipoDeModeloRepository;
  private EventoModeloRepository eventoModeloRepository;
  private GerenciarModelosUseCase useCase;

  @BeforeEach
  void setUp() {
    modeloRepository = mock(ModeloRepository.class);
    usuarioRepository = mock(UsuarioRepository.class);
    maquinaRepository = mock(MaquinaRepository.class);
    tipoDeModeloRepository = mock(TipoDeModeloRepository.class);
    when(tipoDeModeloRepository.existsByNomeAndAtivoTrue(any())).thenReturn(true);
    eventoModeloRepository = mock(EventoModeloRepository.class);
    when(maquinaRepository.existsByNomeAndAtivoTrue(any())).thenReturn(true);
    useCase =
        new GerenciarModelosUseCase(
            modeloRepository,
            usuarioRepository,
            maquinaRepository,
            tipoDeModeloRepository,
            eventoModeloRepository);
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
    when(modeloRepository.maiorVersao("FBOX", "MOD-01")).thenReturn(0);
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
    when(modeloRepository.maiorVersao("FBOX", "MOD-02")).thenReturn(0);
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

  private Modelo modeloExistente(final String codigo, final String maquina) {
    final Instant agora = Instant.now();
    return new Modelo(
        UUID.randomUUID(),
        codigo,
        3,
        "Desc",
        null,
        null,
        null,
        true,
        maquina,
        null,
        false,
        agora,
        agora);
  }

  @Test
  void shouldUseNextAfterLargestVersionAndTrimMachineWhenCreating() {
    // Arrange
    final Usuario gestor = criarGestor();
    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(modeloRepository.maiorVersao("FBOX", "MOD-09")).thenReturn(3);
    when(modeloRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // Act
    final Modelo resultado =
        useCase.criar(
            new GerenciarModelosUseCase.CriarInput(
                "MOD-09", "Descricao", null, "  FBOX ", null, gestor.getId()));

    // Assert
    assertEquals(4, resultado.getVersao());
    assertEquals("FBOX", resultado.getMaquina());
    verify(modeloRepository, times(1)).maiorVersao("FBOX", "MOD-09");
  }

  @Test
  void shouldEditDescriptionWhenMachineIsInactiveButUnchanged() {
    // Arrange
    final Usuario gestor = criarGestor();
    final Modelo modelo = modeloExistente("MOD-01", "ANTIGA");
    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(modeloRepository.findById(modelo.getId())).thenReturn(Optional.of(modelo));
    when(maquinaRepository.existsByNomeAndAtivoTrue("ANTIGA")).thenReturn(false);
    when(modeloRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // Act
    final Modelo resultado =
        useCase.editar(
            new GerenciarModelosUseCase.EditarInput(
                modelo.getId(), "MOD-01", "Nova descricao", null, "ANTIGA", null, gestor.getId()));

    // Assert
    assertEquals("Nova descricao", resultado.getDescricao());
    verify(maquinaRepository, never()).existsByNomeAndAtivoTrue(any());
  }

  @Test
  void shouldRejectEditWhenChangingToInactiveMachine() {
    // Arrange
    final Usuario gestor = criarGestor();
    final Modelo modelo = modeloExistente("MOD-01", "FBOX");
    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(modeloRepository.findById(modelo.getId())).thenReturn(Optional.of(modelo));
    when(maquinaRepository.existsByNomeAndAtivoTrue("INATIVA")).thenReturn(false);

    // Act
    final Executable acao =
        () ->
            useCase.editar(
                new GerenciarModelosUseCase.EditarInput(
                    modelo.getId(), "MOD-01", "Desc", null, "INATIVA", null, gestor.getId()));

    // Assert
    assertThrows(ValidationException.class, acao);
    verify(modeloRepository, never()).save(any());
  }

  @Test
  void shouldRejectEditWithConflictWhenIdentificationAlreadyExists() {
    // Arrange
    final Usuario gestor = criarGestor();
    final Modelo modelo = modeloExistente("MOD-B", "FBOX");
    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(modeloRepository.findById(modelo.getId())).thenReturn(Optional.of(modelo));
    when(modeloRepository.existeOutroComIdentificacao("FBOX", "MOD-A", 3, modelo.getId()))
        .thenReturn(true);

    // Act
    final ConflitoException erro =
        assertThrows(
            ConflitoException.class,
            () ->
                useCase.editar(
                    new GerenciarModelosUseCase.EditarInput(
                        modelo.getId(), "MOD-A", "Desc", null, "FBOX", null, gestor.getId())));

    // Assert
    assertEquals("Ja existe o modelo MOD-A versao 3 na maquina FBOX", erro.getMessage());
    verify(modeloRepository, never()).save(any());
  }

  @Test
  void shouldNotCheckIdentificationWhenCodeAndMachineAreUnchanged() {
    // Arrange
    final Usuario gestor = criarGestor();
    final Modelo modelo = modeloExistente("MOD-01", "FBOX");
    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(modeloRepository.findById(modelo.getId())).thenReturn(Optional.of(modelo));
    when(modeloRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // Act
    useCase.editar(
        new GerenciarModelosUseCase.EditarInput(
            modelo.getId(), "MOD-01", "Outra", null, "FBOX", null, gestor.getId()));

    // Assert
    verify(modeloRepository, never()).existeOutroComIdentificacao(any(), any(), anyInt(), any());
  }

  @Test
  void shouldRejectCreationWhenTipoIsNotInTheCatalog() {
    // Arrange
    final Usuario gestor = criarGestor();
    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(tipoDeModeloRepository.existsByNomeAndAtivoTrue("INVENTADO")).thenReturn(false);

    // Act
    final var acao =
        (Executable)
            () ->
                useCase.criar(
                    new GerenciarModelosUseCase.CriarInput(
                        "MOD-01", "Desc", null, "FBOX", "INVENTADO", gestor.getId()));

    // Assert
    assertThrows(ValidationException.class, acao);
    verify(modeloRepository, never()).save(any());
  }

  @Test
  void shouldSaveTipoFromCatalogWhenCreating() {
    // Arrange
    final Usuario gestor = criarGestor();
    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(modeloRepository.maiorVersao("FBOX", "MOD-02")).thenReturn(0);
    when(modeloRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // Act
    final Modelo resultado =
        useCase.criar(
            new GerenciarModelosUseCase.CriarInput(
                "MOD-02", "Desc", null, "FBOX", " RESINA ", gestor.getId()));

    // Assert
    assertEquals("RESINA", resultado.getTipo());
    verify(tipoDeModeloRepository, times(1)).existsByNomeAndAtivoTrue("RESINA");
  }

  @Test
  void shouldAllowEditWhenTipoIsUnchangedEvenIfInactive() {
    // Arrange
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
            "ANTIGO",
            false,
            agora,
            agora);
    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(modeloRepository.findById(modelo.getId())).thenReturn(Optional.of(modelo));
    when(tipoDeModeloRepository.existsByNomeAndAtivoTrue("ANTIGO")).thenReturn(false);
    when(modeloRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // Act
    final Modelo editado =
        useCase.editar(
            new GerenciarModelosUseCase.EditarInput(
                modelo.getId(), "MOD-01", "Nova", null, "FBOX", "ANTIGO", gestor.getId()));

    // Assert
    assertEquals("Nova", editado.getDescricao());
    verify(tipoDeModeloRepository, never()).existsByNomeAndAtivoTrue(any());
  }
}
