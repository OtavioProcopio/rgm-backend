package com.rgm.api.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rgm.api.adapter.out.persistence.entity.EventoModeloEvidenciaJpaEntity;
import com.rgm.api.adapter.out.persistence.repository.EventoModeloEvidenciaJpaRepository;
import com.rgm.api.core.domain.model.entities.EventoModeloEvidencia;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventoModeloEvidenciaRepositoryAdapterTest {

  private EventoModeloEvidenciaJpaRepository jpa;
  private EventoModeloEvidenciaRepositoryAdapter adapter;

  @BeforeEach
  void setUp() {
    jpa = mock(EventoModeloEvidenciaJpaRepository.class);
    adapter = new EventoModeloEvidenciaRepositoryAdapter(jpa);
  }

  private EventoModeloEvidenciaJpaEntity criarEntity(
      final UUID eventoModeloId, final UUID evidenciaId) {
    final EventoModeloEvidenciaJpaEntity e = new EventoModeloEvidenciaJpaEntity();
    e.setId(UUID.randomUUID());
    e.setEventoModeloId(eventoModeloId);
    e.setEvidenciaId(evidenciaId);
    return e;
  }

  @Test
  void save_persisteERetorna() {
    final UUID eventoId = UUID.randomUUID();
    final UUID evId = UUID.randomUUID();
    final EventoModeloEvidencia domain = new EventoModeloEvidencia(eventoId, evId);
    final EventoModeloEvidenciaJpaEntity saved = criarEntity(eventoId, evId);
    when(jpa.save(any(EventoModeloEvidenciaJpaEntity.class))).thenReturn(saved);

    final EventoModeloEvidencia result = adapter.save(domain);

    assertNotNull(result);
    assertEquals(eventoId, result.getEventoModeloId());
    assertEquals(evId, result.getEvidenciaId());
    verify(jpa).save(any(EventoModeloEvidenciaJpaEntity.class));
  }

  @Test
  void findByEventoModeloId_retornaLista() {
    final UUID eventoId = UUID.randomUUID();
    final UUID evId = UUID.randomUUID();
    when(jpa.findByEventoModeloId(eventoId)).thenReturn(List.of(criarEntity(eventoId, evId)));

    final List<EventoModeloEvidencia> result = adapter.findByEventoModeloId(eventoId);

    assertEquals(1, result.size());
    assertEquals(eventoId, result.get(0).getEventoModeloId());
    assertEquals(evId, result.get(0).getEvidenciaId());
  }

  @Test
  void findByEventoModeloId_quandoVazio_retornaListaVazia() {
    final UUID eventoId = UUID.randomUUID();
    when(jpa.findByEventoModeloId(eventoId)).thenReturn(List.of());

    assertTrue(adapter.findByEventoModeloId(eventoId).isEmpty());
  }

  @Test
  void existsByEvidenciaIdAndEventoModeloModeloId_retornaTrue() {
    final UUID evidenciaId = UUID.randomUUID();
    final UUID modeloId = UUID.randomUUID();
    when(jpa.existsByEvidenciaIdAndEventoModeloModeloId(evidenciaId, modeloId)).thenReturn(true);

    assertTrue(adapter.existsByEvidenciaIdAndEventoModeloModeloId(evidenciaId, modeloId));
  }

  @Test
  void existsByEvidenciaIdAndEventoModeloModeloId_retornaFalse() {
    final UUID evidenciaId = UUID.randomUUID();
    final UUID modeloId = UUID.randomUUID();
    when(jpa.existsByEvidenciaIdAndEventoModeloModeloId(evidenciaId, modeloId)).thenReturn(false);

    assertFalse(adapter.existsByEvidenciaIdAndEventoModeloModeloId(evidenciaId, modeloId));
  }
}
