package com.rgm.api.core.application.usecases.solicitacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.TransicaoStatusInvalidaException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EnviarParaValidacaoUseCaseTest {

  private SolicitacaoRepository solicitacaoRepository;
  private UsuarioRepository usuarioRepository;
  private SolicitacaoAtribuicaoRepository atribuicaoRepository;
  private AtividadeSolicitacaoRepository atividadeRepository;
  private com.rgm.api.core.domain.ports.repositories.SolicitacaoEvidenciaRepository
      solicitacaoEvidenciaRepository;
  private EnviarParaValidacaoUseCase useCase;

  @BeforeEach
  void setUp() {
    solicitacaoRepository = mock(SolicitacaoRepository.class);
    usuarioRepository = mock(UsuarioRepository.class);
    atribuicaoRepository = mock(SolicitacaoAtribuicaoRepository.class);
    atividadeRepository = mock(AtividadeSolicitacaoRepository.class);
    solicitacaoEvidenciaRepository =
        mock(com.rgm.api.core.domain.ports.repositories.SolicitacaoEvidenciaRepository.class);

    // Default mock: return a list containing an evidence so existing transition tests succeed
    when(solicitacaoEvidenciaRepository.findBySolicitacaoId(any()))
        .thenReturn(
            List.of(mock(com.rgm.api.core.domain.model.entities.SolicitacaoEvidencia.class)));

    useCase =
        new EnviarParaValidacaoUseCase(
            solicitacaoRepository,
            usuarioRepository,
            atribuicaoRepository,
            atividadeRepository,
            solicitacaoEvidenciaRepository);
  }

  private Solicitacao criarSolicitacaoEmAndamento() {
    final Instant agora = Instant.now();
    return new Solicitacao(
        UUID.randomUUID(),
        "Titulo",
        "Desc",
        TipoSolicitacao.REPARO,
        StatusSolicitacao.EM_ANDAMENTO,
        PrioridadeSolicitacao.ALTA,
        UUID.randomUUID(),
        UUID.randomUUID(),
        null,
        agora,
        agora,
        null,
        null);
  }

  @Test
  void gestorDeveEnviarParaValidacao() {
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
    final Solicitacao solicitacao = criarSolicitacaoEmAndamento();

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
    when(solicitacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(atividadeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final Solicitacao resultado =
        useCase.execute(new EnviarParaValidacaoUseCase.Input(solicitacao.getId(), gestor.getId(), "Serviço realizado com sucesso"));

    assertEquals(StatusSolicitacao.EM_VALIDACAO, resultado.getStatus());
    verify(atividadeRepository, times(2)).save(any());
  }

  @Test
  void operadorAtribuidoDeveEnviarParaValidacao() {
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
    final Solicitacao solicitacao = criarSolicitacaoEmAndamento();

    when(usuarioRepository.findById(operador.getId())).thenReturn(Optional.of(operador));
    when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
    when(atribuicaoRepository.existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(
            solicitacao.getId(), operador.getId()))
        .thenReturn(true);
    when(solicitacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(atividadeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final Solicitacao resultado =
        useCase.execute(
            new EnviarParaValidacaoUseCase.Input(solicitacao.getId(), operador.getId(), "Serviço concluído pelo operador"));

    assertEquals(StatusSolicitacao.EM_VALIDACAO, resultado.getStatus());
  }

  @Test
  void operadorNaoAtribuidoDeveFalhar() {
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
    final Solicitacao solicitacao = criarSolicitacaoEmAndamento();

    when(usuarioRepository.findById(operador.getId())).thenReturn(Optional.of(operador));
    when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
    when(atribuicaoRepository.existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(
            solicitacao.getId(), operador.getId()))
        .thenReturn(false);

    assertThrows(
        NaoAutorizadoException.class,
        () ->
            useCase.execute(
                new EnviarParaValidacaoUseCase.Input(solicitacao.getId(), operador.getId(), "Serviço concluído pelo operador")));
  }

  @Test
  void deveFalharComTransicaoInvalida() {
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
    final Solicitacao solicitacaoAFazer =
        new Solicitacao(
            UUID.randomUUID(),
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

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(solicitacaoRepository.findById(solicitacaoAFazer.getId()))
        .thenReturn(Optional.of(solicitacaoAFazer));

    assertThrows(
        TransicaoStatusInvalidaException.class,
        () ->
            useCase.execute(
                new EnviarParaValidacaoUseCase.Input(solicitacaoAFazer.getId(), gestor.getId(), "Comentário de teste")));
  }

  @Test
  void deveFalharAoEnviarParaValidacaoSeReparoSemEvidencia() {
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
    final Solicitacao solicitacao = criarSolicitacaoEmAndamento();

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
    // Simulate no uploaded files (empty list)
    when(solicitacaoEvidenciaRepository.findBySolicitacaoId(solicitacao.getId()))
        .thenReturn(java.util.Collections.emptyList());

    final var ex =
        assertThrows(
            com.rgm.api.core.domain.exceptions.BusinessRuleException.class,
            () ->
                useCase.execute(
                    new EnviarParaValidacaoUseCase.Input(solicitacao.getId(), gestor.getId(), "Serviço realizado com sucesso")));

    assertTrue(ex.getMessage().contains("exigem o anexo de pelo menos 1 evidência"));
  }
}
