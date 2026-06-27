package com.rgm.api.core.application.usecases.solicitacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AbrirSolicitacaoUseCaseTest {

  private SolicitacaoRepository solicitacaoRepository;
  private ModeloRepository modeloRepository;
  private AtividadeSolicitacaoRepository atividadeRepository;
  private UsuarioRepository usuarioRepository;
  private AbrirSolicitacaoUseCase useCase;

  private final String maquina = "FBOX";
  private final UUID usuarioId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    solicitacaoRepository = mock(SolicitacaoRepository.class);
    modeloRepository = mock(ModeloRepository.class);
    atividadeRepository = mock(AtividadeSolicitacaoRepository.class);
    usuarioRepository = mock(UsuarioRepository.class);
    useCase =
        new AbrirSolicitacaoUseCase(
            solicitacaoRepository, modeloRepository, atividadeRepository, usuarioRepository);
  }

  private Modelo criarModelo(final boolean ativo, final boolean temPendencia) {
    final Instant agora = Instant.now();
    return new Modelo(
        UUID.randomUUID(),
        "MOD-001",
        1,
        "Modelo Teste",
        null,
        null,
        null,
        null,
        null,
        ativo,
        maquina,
        temPendencia,
        agora,
        agora);
  }

  private void mockUsuario(final UUID id, final PerfilUsuario perfil) {
    final Usuario usuario;
    if (perfil == PerfilUsuario.EXTERNO) {
      usuario = Usuario.criarExterno("Test", Instant.now());
    } else {
      usuario = Usuario.criarInterno("Test", "t@t.com", "hash", perfil, Instant.now());
    }
    when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));
  }

  @Test
  void deveAbrirSolicitacaoComSucesso() {
    mockUsuario(usuarioId, PerfilUsuario.OPERADOR);
    final Modelo modelo = criarModelo(true, false);
    when(modeloRepository.findById(modelo.getId())).thenReturn(Optional.of(modelo));
    when(solicitacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(atividadeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final AbrirSolicitacaoUseCase.Input input =
        new AbrirSolicitacaoUseCase.Input(
            "Titulo", "Descricao", TipoSolicitacao.REPARO, modelo.getId(), usuarioId);

    final Solicitacao resultado = useCase.execute(input);

    assertNotNull(resultado);
    assertEquals(StatusSolicitacao.A_FAZER, resultado.getStatus());
    assertEquals("Titulo", resultado.getTitulo());
    verify(solicitacaoRepository).save(any(Solicitacao.class));
    verify(atividadeRepository).save(any(AtividadeSolicitacao.class));
    verify(modeloRepository).save(any(Modelo.class));
  }

  @Test
  void naoDeveAtualizarPendenciaSeJaAberta() {
    mockUsuario(usuarioId, PerfilUsuario.GESTOR);
    final Modelo modelo = criarModelo(true, true);
    when(modeloRepository.findById(modelo.getId())).thenReturn(Optional.of(modelo));
    when(solicitacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(atividadeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final AbrirSolicitacaoUseCase.Input input =
        new AbrirSolicitacaoUseCase.Input(
            "Titulo", "Descricao", TipoSolicitacao.REPARO, modelo.getId(), usuarioId);

    useCase.execute(input);

    verify(modeloRepository, never()).save(any(Modelo.class));
  }

  @Test
  void deveFalharComModeloNaoEncontrado() {
    mockUsuario(usuarioId, PerfilUsuario.OPERADOR);
    final UUID modeloId = UUID.randomUUID();
    when(modeloRepository.findById(modeloId)).thenReturn(Optional.empty());

    assertThrows(
        RecursoNaoEncontradoException.class,
        () ->
            useCase.execute(
                new AbrirSolicitacaoUseCase.Input(
                    "Titulo", "Desc", TipoSolicitacao.REPARO, modeloId, usuarioId)));
  }

  @Test
  void deveFalharComModeloInativo() {
    mockUsuario(usuarioId, PerfilUsuario.OPERADOR);
    final Modelo modelo = criarModelo(false, false);
    when(modeloRepository.findById(modelo.getId())).thenReturn(Optional.of(modelo));

    assertThrows(
        BusinessRuleException.class,
        () ->
            useCase.execute(
                new AbrirSolicitacaoUseCase.Input(
                    "Titulo", "Desc", TipoSolicitacao.REPARO, modelo.getId(), usuarioId)));
  }

  @Test
  void deveFalharComPerfilExterno() {
    mockUsuario(usuarioId, PerfilUsuario.EXTERNO);

    assertThrows(
        NaoAutorizadoException.class,
        () ->
            useCase.execute(
                new AbrirSolicitacaoUseCase.Input(
                    "Titulo", "Desc", TipoSolicitacao.REPARO, UUID.randomUUID(), usuarioId)));
  }
}
