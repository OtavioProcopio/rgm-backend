package com.rgm.api.core.application.usecases.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.rgm.api.core.application.usecases.modelo.RecalcularPendenciaUseCase;
import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Evidencia;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.entities.SolicitacaoEvidencia;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.EventoModeloRepository;
import com.rgm.api.core.domain.ports.repositories.EvidenciaRepository;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoEvidenciaRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.StorageService;
import java.time.Instant;
import java.util.List;
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
  private RecalcularPendenciaUseCase recalcularPendenciaUseCase;
  private EvidenciaRepository evidenciaRepository;
  private StorageService storageService;
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
    recalcularPendenciaUseCase = mock(RecalcularPendenciaUseCase.class);
    evidenciaRepository = mock(EvidenciaRepository.class);
    storageService = mock(StorageService.class);
    useCase =
        new ExcluirRegistroUseCase(
            usuarioRepository,
            solicitacaoRepository,
            modeloRepository,
            atribuicaoRepository,
            atividadeRepository,
            solicitacaoEvidenciaRepository,
            eventoModeloRepository,
            recalcularPendenciaUseCase,
            evidenciaRepository,
            storageService);
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

    final UUID evidenciaId = UUID.randomUUID();
    final SolicitacaoEvidencia rel = new SolicitacaoEvidencia(solId, evidenciaId);
    final Evidencia ev =
        new Evidencia(
            evidenciaId, "http://file", "image/png", "foto.png", 1024, UUID.randomUUID(), agora);

    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(solicitacaoRepository.findById(solId)).thenReturn(Optional.of(sol));
    when(solicitacaoEvidenciaRepository.findBySolicitacaoId(solId)).thenReturn(List.of(rel));
    when(evidenciaRepository.findById(evidenciaId)).thenReturn(Optional.of(ev));

    useCase.execute(
        new ExcluirRegistroUseCase.Input(
            ExcluirRegistroUseCase.TipoRecurso.SOLICITACAO, solId, admin.getId()));

    verify(storageService).delete("http://file");
    verify(evidenciaRepository).deleteById(evidenciaId);
    verify(atribuicaoRepository).deleteBySolicitacaoId(solId);
    verify(atividadeRepository).deleteBySolicitacaoId(solId);
    verify(solicitacaoEvidenciaRepository).deleteBySolicitacaoId(solId);
    verify(solicitacaoRepository).deleteById(solId);
    verify(recalcularPendenciaUseCase).execute(sol.getModeloId());
  }

  @Test
  void deveExcluirSolicitacaoTerminalSemRecalcularPendencia() {
    final Usuario admin = criarAdmin();
    final Instant agora = Instant.now();
    final UUID solId = UUID.randomUUID();
    final Solicitacao sol =
        new Solicitacao(
            solId,
            "T",
            "D",
            TipoSolicitacao.REPARO,
            StatusSolicitacao.CONCLUIDA,
            PrioridadeSolicitacao.MEDIA,
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Feito",
            agora,
            agora,
            agora,
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
    verifyNoInteractions(recalcularPendenciaUseCase);
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

  @Test
  void deveFalharAoExcluirAdminSiMesmo() {
    final Usuario admin = criarAdmin();
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

    assertThrows(
        BusinessRuleException.class,
        () ->
            useCase.execute(
                new ExcluirRegistroUseCase.Input(
                    ExcluirRegistroUseCase.TipoRecurso.USUARIO, admin.getId(), admin.getId())));
  }

  @Test
  void deveFalharAoExcluirModeloInexistente() {
    final Usuario admin = criarAdmin();
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(modeloRepository.findById(any())).thenReturn(Optional.empty());

    assertThrows(
        RecursoNaoEncontradoException.class,
        () ->
            useCase.execute(
                new ExcluirRegistroUseCase.Input(
                    ExcluirRegistroUseCase.TipoRecurso.MODELO, UUID.randomUUID(), admin.getId())));
  }

  @Test
  void deveFalharAoExcluirUsuarioInexistente() {
    final Usuario admin = criarAdmin();
    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(usuarioRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

    assertThrows(
        RecursoNaoEncontradoException.class,
        () ->
            useCase.execute(
                new ExcluirRegistroUseCase.Input(
                    ExcluirRegistroUseCase.TipoRecurso.USUARIO, UUID.randomUUID(), admin.getId())));
  }

  @Test
  void deveFalharAoExcluirUsuarioComAtribuicao() {
    final Usuario admin = criarAdmin();
    final UUID usuarioId = UUID.randomUUID();
    final Instant agora = Instant.now();
    final Usuario usuario =
        new Usuario(
            usuarioId, "User", "user@test.com", "hash", PerfilUsuario.OPERADOR, true, agora, agora);

    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
    when(solicitacaoRepository.existsByAbertaPorUsuarioId(usuarioId)).thenReturn(false);
    when(atribuicaoRepository.existsByUsuarioIdAndRemovidoEmIsNull(usuarioId)).thenReturn(true);

    assertThrows(
        BusinessRuleException.class,
        () ->
            useCase.execute(
                new ExcluirRegistroUseCase.Input(
                    ExcluirRegistroUseCase.TipoRecurso.USUARIO, usuarioId, admin.getId())));
  }

  @Test
  void deveFalharAoExcluirUsuarioComAtividades() {
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
    when(atividadeRepository.existsByAutorId(usuarioId)).thenReturn(true);

    assertThrows(
        BusinessRuleException.class,
        () ->
            useCase.execute(
                new ExcluirRegistroUseCase.Input(
                    ExcluirRegistroUseCase.TipoRecurso.USUARIO, usuarioId, admin.getId())));
  }

  @Test
  void deveFalharAoExcluirUsuarioComEventosDeModelo() {
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
    when(eventoModeloRepository.existsByExecutadoPorUsuarioId(usuarioId)).thenReturn(true);

    assertThrows(
        BusinessRuleException.class,
        () ->
            useCase.execute(
                new ExcluirRegistroUseCase.Input(
                    ExcluirRegistroUseCase.TipoRecurso.USUARIO, usuarioId, admin.getId())));
  }
}
