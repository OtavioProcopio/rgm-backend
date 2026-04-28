package com.rgm.api.core.application.usecases.solicitacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AbrirSolicitacaoUseCaseTest {

  private SolicitacaoRepository solicitacaoRepository;
  private ModeloRepository modeloRepository;
  private AtividadeSolicitacaoRepository atividadeRepository;
  private AbrirSolicitacaoUseCase useCase;

  private final UUID maquinaId = UUID.randomUUID();
  private final UUID usuarioId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    solicitacaoRepository = mock(SolicitacaoRepository.class);
    modeloRepository = mock(ModeloRepository.class);
    atividadeRepository = mock(AtividadeSolicitacaoRepository.class);
    useCase =
        new AbrirSolicitacaoUseCase(solicitacaoRepository, modeloRepository, atividadeRepository);
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
        maquinaId,
        temPendencia,
        agora,
        agora);
  }

  @Test
  void deveAbrirSolicitacaoComSucesso() {
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
    final Modelo modelo = criarModelo(false, false);
    when(modeloRepository.findById(modelo.getId())).thenReturn(Optional.of(modelo));

    assertThrows(
        ValidationException.class,
        () ->
            useCase.execute(
                new AbrirSolicitacaoUseCase.Input(
                    "Titulo", "Desc", TipoSolicitacao.REPARO, modelo.getId(), usuarioId)));
  }
}
