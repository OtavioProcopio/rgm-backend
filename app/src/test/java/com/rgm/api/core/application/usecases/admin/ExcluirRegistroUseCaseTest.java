package com.rgm.api.core.application.usecases.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.EventoModeloRepository;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoEvidenciaRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExcluirRegistroUseCaseTest {

  private UsuarioRepository usuarioRepository;
  private SolicitacaoRepository solicitacaoRepository;
  private ModeloRepository modeloRepository;
  private SolicitacaoAtribuicaoRepository atribuicaoRepository;
  private AtividadeSolicitacaoRepository atividadeRepository;
  private SolicitacaoEvidenciaRepository solicitacaoEvidenciaRepository;
  private EventoModeloRepository eventoModeloRepository;
  private ExcluirRegistroUseCase useCase;

  @BeforeEach
  void setUp() {
    usuarioRepository = mock(UsuarioRepository.class);
    solicitacaoRepository = mock(SolicitacaoRepository.class);
    modeloRepository = mock(ModeloRepository.class);
    atribuicaoRepository = mock(SolicitacaoAtribuicaoRepository.class);
    atividadeRepository = mock(AtividadeSolicitacaoRepository.class);
    solicitacaoEvidenciaRepository = mock(SolicitacaoEvidenciaRepository.class);
    eventoModeloRepository = mock(EventoModeloRepository.class);
    useCase =
        new ExcluirRegistroUseCase(
            usuarioRepository,
            solicitacaoRepository,
            modeloRepository,
            atribuicaoRepository,
            atividadeRepository,
            solicitacaoEvidenciaRepository,
            eventoModeloRepository);
  }

  private Usuario criarAdmin() {
    final Instant ago = Instant.now();
    return new Usuario(
        UUID.randomUUID(),
        "Admin",
        "admin@test.com",
        "hash",
        PerfilUsuario.ADMINISTRADOR,
        true,
        ago,
        ago);
  }

  @Test
  void deveExcluirSolicitacaoEmCascata() {
    final Usuario admin = criarAdmin();
    final Instant agora = Instant.now();
    final UUID solId = UUID.randomUUID();
    final Solicitacao sol =
        new Solicitacao(
            solId,
            "T",
            "D",
            TipoSolicitacao.REPARO,
            StatusSolicitacao.A_FAZER,
            null,
            UUID.randomUUID(),
            UUID.randomUUID(),
            null,
            agora,
            agora,
            null,
            null);

    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(solicitacaoRepository.findById(solId)).thenReturn(Optional.of(sol));

    useCase.execute(
        new ExcluirRegistroUseCase.Input(
            ExcluirRegistroUseCase.TipoRecurso.SOLICITACAO, solId, admin.getId()));

    verify(atribuicaoRepository).deleteBySolicitacaoId(solId);
    verify(atividadeRepository).deleteBySolicitacaoId(solId);
    verify(solicitacaoEvidenciaRepository).deleteBySolicitacaoId(solId);
    verify(solicitacaoRepository).deleteById(solId);
  }

  @Test
  void deveExcluirModelo() {
    final Usuario admin = criarAdmin();
    final UUID modeloId = UUID.randomUUID();
    final Instant agora = Instant.now();
    final Modelo modelo =
        new Modelo(
            modeloId,
            "M1",
            1,
            "Modelo 1",
            null,
            null,
            null,
            null,
            null,
            true,
            "FBOX",
            false,
            agora,
            agora);

    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(modeloRepository.findById(modeloId)).thenReturn(Optional.of(modelo));
    when(solicitacaoRepository.existsByModeloId(modeloId)).thenReturn(false);

    useCase.execute(
        new ExcluirRegistroUseCase.Input(
            ExcluirRegistroUseCase.TipoRecurso.MODELO, modeloId, admin.getId()));

    verify(modeloRepository).deleteById(modeloId);
  }

  @Test
  void deveExcluirUsuario() {
    final Usuario admin = criarAdmin();
    final UUID usuarioId = UUID.randomUUID();
    final Instant agora = Instant.now();
    final Usuario usuario =
        new Usuario(
            usuarioId, "User", "user@test.com", "hash", PerfilUsuario.OPERADOR, true, agora, agora);

    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
    when(solicitacaoRepository.existsByAbertaPorUsuarioId(usuarioId)).thenReturn(false);
    when(atribuicaoRepository.existsByUsuarioIdAndRemovidoEmIsNull(usuarioId)).thenReturn(false);
    when(atividadeRepository.existsByAutorId(usuarioId)).thenReturn(false);
    when(eventoModeloRepository.existsByExecutadoPorUsuarioId(usuarioId)).thenReturn(false);

    useCase.execute(
        new ExcluirRegistroUseCase.Input(
            ExcluirRegistroUseCase.TipoRecurso.USUARIO, usuarioId, admin.getId()));

    verify(usuarioRepository).deleteById(usuarioId);
  }

  @Test
  void operadorNaoDeveExcluir() {
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
            useCase.execute(
                new ExcluirRegistroUseCase.Input(
                    ExcluirRegistroUseCase.TipoRecurso.SOLICITACAO,
                    UUID.randomUUID(),
                    operador.getId())));
  }

  @Test
  void deveFalharComSolicitacaoNaoEncontrada() {
    final Usuario admin = criarAdmin();
    final UUID solId = UUID.randomUUID();

    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(solicitacaoRepository.findById(solId)).thenReturn(Optional.empty());

    assertThrows(
        RecursoNaoEncontradoException.class,
        () ->
            useCase.execute(
                new ExcluirRegistroUseCase.Input(
                    ExcluirRegistroUseCase.TipoRecurso.SOLICITACAO, solId, admin.getId())));
  }

  @Test
  void deveFalharAoExcluirModeloComSolicitacoesVinculadas() {
    final Usuario admin = criarAdmin();
    final UUID modeloId = UUID.randomUUID();
    final Instant agora = Instant.now();
    final Modelo modelo =
        new Modelo(
            modeloId, "M1", 1, "Mod1", null, null, null, null, null, true, "FBOX", false, agora,
            agora);

    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(modeloRepository.findById(modeloId)).thenReturn(Optional.of(modelo));
    when(solicitacaoRepository.existsByModeloId(modeloId)).thenReturn(true);

    assertThrows(
        BusinessRuleException.class,
        () ->
            useCase.execute(
                new ExcluirRegistroUseCase.Input(
                    ExcluirRegistroUseCase.TipoRecurso.MODELO, modeloId, admin.getId())));
  }

  @Test
  void deveFalharAoExcluirUsuarioComHistoricoDeSolicitacoes() {
    final Usuario admin = criarAdmin();
    final UUID usuarioId = UUID.randomUUID();
    final Instant agora = Instant.now();
    final Usuario usuario =
        new Usuario(
            usuarioId, "User", "user@test.com", "hash", PerfilUsuario.OPERADOR, true, agora, agora);

    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
    when(solicitacaoRepository.existsByAbertaPorUsuarioId(usuarioId)).thenReturn(true);

    assertThrows(
        BusinessRuleException.class,
        () ->
            useCase.execute(
                new ExcluirRegistroUseCase.Input(
                    ExcluirRegistroUseCase.TipoRecurso.USUARIO, usuarioId, admin.getId())));
  }
}
