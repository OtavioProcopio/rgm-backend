package com.rgm.api.core.application.usecases.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.MaquinaRepository;
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
  private MaquinaRepository maquinaRepository;
  private SolicitacaoAtribuicaoRepository atribuicaoRepository;
  private AtividadeSolicitacaoRepository atividadeRepository;
  private SolicitacaoEvidenciaRepository solicitacaoEvidenciaRepository;
  private ExcluirRegistroUseCase useCase;

  @BeforeEach
  void setUp() {
    usuarioRepository = mock(UsuarioRepository.class);
    solicitacaoRepository = mock(SolicitacaoRepository.class);
    modeloRepository = mock(ModeloRepository.class);
    maquinaRepository = mock(MaquinaRepository.class);
    atribuicaoRepository = mock(SolicitacaoAtribuicaoRepository.class);
    atividadeRepository = mock(AtividadeSolicitacaoRepository.class);
    solicitacaoEvidenciaRepository = mock(SolicitacaoEvidenciaRepository.class);
    useCase =
        new ExcluirRegistroUseCase(
            usuarioRepository,
            solicitacaoRepository,
            modeloRepository,
            maquinaRepository,
            atribuicaoRepository,
            atividadeRepository,
            solicitacaoEvidenciaRepository);
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

    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

    useCase.execute(
        new ExcluirRegistroUseCase.Input(
            ExcluirRegistroUseCase.TipoRecurso.MODELO, modeloId, admin.getId()));

    verify(modeloRepository).deleteById(modeloId);
  }

  @Test
  void deveExcluirUsuario() {
    final Usuario admin = criarAdmin();
    final UUID usuarioId = UUID.randomUUID();

    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

    useCase.execute(
        new ExcluirRegistroUseCase.Input(
            ExcluirRegistroUseCase.TipoRecurso.USUARIO, usuarioId, admin.getId()));

    verify(usuarioRepository).deleteById(usuarioId);
  }

  @Test
  void deveExcluirMaquina() {
    final Usuario admin = criarAdmin();
    final UUID maquinaId = UUID.randomUUID();

    when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

    useCase.execute(
        new ExcluirRegistroUseCase.Input(
            ExcluirRegistroUseCase.TipoRecurso.MAQUINA, maquinaId, admin.getId()));

    verify(maquinaRepository).deleteById(maquinaId);
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
        ValidationException.class,
        () ->
            useCase.execute(
                new ExcluirRegistroUseCase.Input(
                    ExcluirRegistroUseCase.TipoRecurso.SOLICITACAO, solId, admin.getId())));
  }
}
