package com.rgm.api.core.application.usecases.evidencia;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Evidencia;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.EvidenciaRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoEvidenciaRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExcluirEvidenciaUseCaseTest {

  @Mock private SolicitacaoRepository solicitacaoRepository;
  @Mock private EvidenciaRepository evidenciaRepository;
  @Mock private SolicitacaoEvidenciaRepository solicitacaoEvidenciaRepository;
  @Mock private UsuarioRepository usuarioRepository;
  @Mock private SolicitacaoAtribuicaoRepository atribuicaoRepository;
  @InjectMocks private ExcluirEvidenciaUseCase useCase;

  private Solicitacao solicitacaoAberta() {
    return Solicitacao.abrir(
        "T", "D", TipoSolicitacao.REPARO, UUID.randomUUID(), UUID.randomUUID(), Instant.now());
  }

  private Evidencia evidencia(final UUID enviadaPor) {
    return new Evidencia(
        UUID.randomUUID(), "http://url", "image/jpeg", "foto.jpg", 100, enviadaPor, Instant.now());
  }

  private Usuario gestor(final UUID id) {
    return new Usuario(
        id,
        "Gestor",
        "gestor@rgm.com",
        "hash",
        PerfilUsuario.GESTOR,
        true,
        Instant.now(),
        Instant.now());
  }

  private Usuario operador(final UUID id) {
    return new Usuario(
        id, "Op", "op@rgm.com", "hash", PerfilUsuario.OPERADOR, true, Instant.now(), Instant.now());
  }

  @Test
  void execute_gestorPodeExcluirQualquerEvidencia() {
    final UUID solId = UUID.randomUUID();
    final UUID evId = UUID.randomUUID();
    final UUID gestorId = UUID.randomUUID();
    final Solicitacao sol = solicitacaoAberta();
    final Evidencia ev = evidencia(UUID.randomUUID());

    when(solicitacaoRepository.findById(solId)).thenReturn(Optional.of(sol));
    when(evidenciaRepository.findById(evId)).thenReturn(Optional.of(ev));
    when(solicitacaoEvidenciaRepository.existsBySolicitacaoIdAndEvidenciaId(solId, evId))
        .thenReturn(true);
    when(usuarioRepository.findById(gestorId)).thenReturn(Optional.of(gestor(gestorId)));

    useCase.execute(new ExcluirEvidenciaUseCase.Input(solId, evId, gestorId));

    verify(solicitacaoEvidenciaRepository).deleteByEvidenciaId(evId);
    verify(evidenciaRepository).deleteById(evId);
  }

  @Test
  void execute_operadorPodeExcluirSuaPropriaEvidencia() {
    final UUID solId = UUID.randomUUID();
    final UUID evId = UUID.randomUUID();
    final UUID operadorId = UUID.randomUUID();
    final Solicitacao sol = solicitacaoAberta();
    final Evidencia ev = evidencia(operadorId);

    when(solicitacaoRepository.findById(solId)).thenReturn(Optional.of(sol));
    when(evidenciaRepository.findById(evId)).thenReturn(Optional.of(ev));
    when(solicitacaoEvidenciaRepository.existsBySolicitacaoIdAndEvidenciaId(solId, evId))
        .thenReturn(true);
    when(usuarioRepository.findById(operadorId)).thenReturn(Optional.of(operador(operadorId)));

    useCase.execute(new ExcluirEvidenciaUseCase.Input(solId, evId, operadorId));

    verify(evidenciaRepository).deleteById(evId);
  }

  @Test
  void execute_solicitacaoTerminalLancaExcecao() {
    final UUID solId = UUID.randomUUID();
    final Solicitacao sol = solicitacaoAberta().cancelar("motivo", Instant.now());

    when(solicitacaoRepository.findById(solId)).thenReturn(Optional.of(sol));

    assertThatThrownBy(
            () ->
                useCase.execute(
                    new ExcluirEvidenciaUseCase.Input(solId, UUID.randomUUID(), UUID.randomUUID())))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void execute_evidenciaNaoEncontradaLancaExcecao() {
    final UUID solId = UUID.randomUUID();
    final UUID evId = UUID.randomUUID();
    when(solicitacaoRepository.findById(solId)).thenReturn(Optional.of(solicitacaoAberta()));
    when(evidenciaRepository.findById(evId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                useCase.execute(new ExcluirEvidenciaUseCase.Input(solId, evId, UUID.randomUUID())))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }

  @Test
  void execute_evidenciaNaoPertenceASolicitacaoLancaExcecao() {
    final UUID solId = UUID.randomUUID();
    final UUID evId = UUID.randomUUID();
    when(solicitacaoRepository.findById(solId)).thenReturn(Optional.of(solicitacaoAberta()));
    when(evidenciaRepository.findById(evId)).thenReturn(Optional.of(evidencia(UUID.randomUUID())));
    when(solicitacaoEvidenciaRepository.existsBySolicitacaoIdAndEvidenciaId(solId, evId))
        .thenReturn(false);

    assertThatThrownBy(
            () ->
                useCase.execute(new ExcluirEvidenciaUseCase.Input(solId, evId, UUID.randomUUID())))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }

  @Test
  void execute_operadorSemAtribuicaoNaoPodeExcluirEvidenciaDeOutroUsuario() {
    final UUID solId = UUID.randomUUID();
    final UUID evId = UUID.randomUUID();
    final UUID operadorId = UUID.randomUUID();
    when(solicitacaoRepository.findById(solId)).thenReturn(Optional.of(solicitacaoAberta()));
    when(evidenciaRepository.findById(evId)).thenReturn(Optional.of(evidencia(UUID.randomUUID())));
    when(solicitacaoEvidenciaRepository.existsBySolicitacaoIdAndEvidenciaId(solId, evId))
        .thenReturn(true);
    when(usuarioRepository.findById(operadorId)).thenReturn(Optional.of(operador(operadorId)));
    when(atribuicaoRepository.existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(
            solId, operadorId))
        .thenReturn(false);

    assertThatThrownBy(
            () -> useCase.execute(new ExcluirEvidenciaUseCase.Input(solId, evId, operadorId)))
        .isInstanceOf(NaoAutorizadoException.class);
  }
}
