package com.rgm.api.core.application.usecases.tipomodelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rgm.api.core.domain.model.aggregates.TipoDeModelo;
import com.rgm.api.core.domain.ports.repositories.TipoDeModeloRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListarTiposDeModeloUseCaseTest {

  @Test
  void listaTodas() {
    final TipoDeModeloRepository repo = mock(TipoDeModeloRepository.class);
    final Instant agora = Instant.now();
    final TipoDeModelo m = new TipoDeModelo(UUID.randomUUID(), "VICK", true, agora, agora);
    when(repo.findAll()).thenReturn(List.of(m));

    final List<TipoDeModelo> result = new ListarTiposDeModeloUseCase(repo).execute();

    assertEquals(1, result.size());
    assertEquals("VICK", result.get(0).getNome());
  }
}
