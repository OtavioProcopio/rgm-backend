package com.rgm.api.core.application.usecases.modelo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecalcularPendenciaUseCaseTest {

  private ModeloRepository modeloRepository;
  private SolicitacaoRepository solicitacaoRepository;
  private RecalcularPendenciaUseCase useCase;

  @BeforeEach
  void setUp() {
    modeloRepository = mock(ModeloRepository.class);
    solicitacaoRepository = mock(SolicitacaoRepository.class);
    useCase = new RecalcularPendenciaUseCase(modeloRepository, solicitacaoRepository);
  }

  private Modelo criarModelo(final boolean temPendencia) {
    final Instant agora = Instant.now();
    return new Modelo(
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
        temPendencia,
        agora,
        agora);
  }

  @Test
  void deveAtualizarDeTrueParaFalse() {
    final Modelo modelo = criarModelo(true);
    when(modeloRepository.findById(modelo.getId())).thenReturn(Optional.of(modelo));
    when(solicitacaoRepository.existsByModeloIdAndStatusIn(eq(modelo.getId()), any()))
        .thenReturn(false);

    useCase.execute(modelo.getId());

    verify(modeloRepository).save(any(Modelo.class));
  }

  @Test
  void deveAtualizarDeFalseParaTrue() {
    final Modelo modelo = criarModelo(false);
    when(modeloRepository.findById(modelo.getId())).thenReturn(Optional.of(modelo));
    when(solicitacaoRepository.existsByModeloIdAndStatusIn(eq(modelo.getId()), any()))
        .thenReturn(true);

    useCase.execute(modelo.getId());

    verify(modeloRepository).save(any(Modelo.class));
  }

  @Test
  void naoDeveAtualizarSeIgual() {
    final Modelo modelo = criarModelo(true);
    when(modeloRepository.findById(modelo.getId())).thenReturn(Optional.of(modelo));
    when(solicitacaoRepository.existsByModeloIdAndStatusIn(eq(modelo.getId()), any()))
        .thenReturn(true);

    useCase.execute(modelo.getId());

    verify(modeloRepository, never()).save(any());
  }
}
