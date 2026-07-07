package com.rgm.api.core.application.usecases.maquina;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rgm.api.core.domain.model.aggregates.Maquina;
import com.rgm.api.core.domain.ports.repositories.MaquinaRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListarMaquinasUseCaseTest {

  @Test
  void listaTodas() {
    final MaquinaRepository repo = mock(MaquinaRepository.class);
    final Instant agora = Instant.now();
    final Maquina m = new Maquina(UUID.randomUUID(), "VICK", true, agora, agora);
    when(repo.findAll()).thenReturn(List.of(m));

    final List<Maquina> result = new ListarMaquinasUseCase(repo).execute();

    assertEquals(1, result.size());
    assertEquals("VICK", result.get(0).getNome());
  }
}
