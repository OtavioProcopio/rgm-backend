package com.rgm.api.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rgm.api.adapter.out.persistence.entity.EventoModeloJpaEntity;
import com.rgm.api.adapter.out.persistence.repository.EventoModeloJpaRepository;
import com.rgm.api.core.domain.model.aggregates.EventoModelo;
import com.rgm.api.core.domain.model.enums.TipoEventoModelo;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventoModeloRepositoryAdapterTest {

  private EventoModeloJpaRepository jpa;
  private EventoModeloRepositoryAdapter adapter;

  @BeforeEach
  void setUp() {
    jpa = mock(EventoModeloJpaRepository.class);
    adapter = new EventoModeloRepositoryAdapter(jpa);
  }

  private EventoModeloJpaEntity criarEntity(final UUID modeloId) {
    final EventoModeloJpaEntity e = new EventoModeloJpaEntity();
    e.setId(UUID.randomUUID());
    e.setModeloId(modeloId);
    e.setTipo(TipoEventoModelo.MANUTENCAO);
    e.setTitulo("Manutenção preventiva");
    e.setDescricao("Desc");
    e.setEstadoModeloDescricao("Operacional");
    e.setDefineFotoCapa(false);
    e.setExecutadoPorUsuarioId(UUID.randomUUID());
    e.setCriadoEm(Instant.now());
    return e;
  }

  private EventoModelo criarDomain(final UUID modeloId) {
    return new EventoModelo(
        UUID.randomUUID(),
        modeloId,
        TipoEventoModelo.MANUTENCAO,
        "Manutenção preventiva",
        "Desc",
        "Operacional",
        false,
        UUID.randomUUID(),
        null,
        Instant.now());
  }

  @Test
  void save_persisteERetorna() {
    final UUID modeloId = UUID.randomUUID();
    final EventoModelo domain = criarDomain(modeloId);
    final EventoModeloJpaEntity e = criarEntity(modeloId);
    when(jpa.save(any(EventoModeloJpaEntity.class))).thenReturn(e);

    final EventoModelo result = adapter.save(domain);

    assertNotNull(result);
    verify(jpa).save(any(EventoModeloJpaEntity.class));
  }

  @Test
  void findByModeloId_retornaLista() {
    final UUID modeloId = UUID.randomUUID();
    when(jpa.findByModeloId(modeloId)).thenReturn(List.of(criarEntity(modeloId)));

    final List<EventoModelo> result = adapter.findByModeloId(modeloId);

    assertEquals(1, result.size());
    assertEquals(modeloId, result.get(0).getModeloId());
  }

  @Test
  void findByModeloId_quandoVazio_retornaListaVazia() {
    final UUID modeloId = UUID.randomUUID();
    when(jpa.findByModeloId(modeloId)).thenReturn(List.of());

    assertTrue(adapter.findByModeloId(modeloId).isEmpty());
  }

  @Test
  void existsByExecutadoPorUsuarioId_quandoExistir_retornaTrue() {
    final UUID userId = UUID.randomUUID();
    when(jpa.existsByExecutadoPorUsuarioId(userId)).thenReturn(true);

    assertTrue(adapter.existsByExecutadoPorUsuarioId(userId));
  }

  @Test
  void existsByExecutadoPorUsuarioId_quandoNaoExistir_retornaFalse() {
    final UUID userId = UUID.randomUUID();
    when(jpa.existsByExecutadoPorUsuarioId(userId)).thenReturn(false);

    assertFalse(adapter.existsByExecutadoPorUsuarioId(userId));
  }
}
