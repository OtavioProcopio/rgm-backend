package com.rgm.api.adapter.out.persistence.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.rgm.api.adapter.out.persistence.entity.*;
import com.rgm.api.core.domain.model.aggregates.*;
import com.rgm.api.core.domain.model.entities.*;
import com.rgm.api.core.domain.model.enums.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MapperTest {

  private static final Instant NOW = Instant.parse("2025-01-01T10:00:00Z");

  // ──────── UsuarioMapper ────────

  @Test
  void usuarioMapper_toJpa_roundtrip() {
    final UUID id = UUID.randomUUID();
    final Usuario u =
        new Usuario(id, "Alice", "alice@x.com", "hash123", PerfilUsuario.OPERADOR, true, NOW, NOW);
    final UsuarioJpaEntity jpa = UsuarioMapper.toJpa(u);

    assertEquals(id, jpa.getId());
    assertEquals("Alice", jpa.getNome());
    assertEquals("alice@x.com", jpa.getEmail());
    assertEquals("hash123", jpa.getSenhaHash());
    assertEquals(PerfilUsuario.OPERADOR, jpa.getPerfil());
    assertTrue(jpa.isAtivo());
    assertEquals(NOW, jpa.getCriadoEm());
    assertEquals(NOW, jpa.getAtualizadoEm());
  }

  @Test
  void usuarioMapper_toDomain_roundtrip() {
    final UUID id = UUID.randomUUID();
    final UsuarioJpaEntity e =
        new UsuarioJpaEntity(
            id, "Bob", "bob@x.com", "hash456", PerfilUsuario.GESTOR, false, NOW, NOW);
    final Usuario u = UsuarioMapper.toDomain(e);

    assertEquals(id, u.getId());
    assertEquals("Bob", u.getNome());
    assertEquals("bob@x.com", u.getEmail());
    assertEquals(PerfilUsuario.GESTOR, u.getPerfil());
    assertFalse(u.isAtivo());
  }

  // ──────── SolicitacaoMapper ────────

  @Test
  void solicitacaoMapper_toJpa_roundtrip() {
    final UUID id = UUID.randomUUID();
    final UUID modeloId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final Solicitacao s =
        new Solicitacao(
            id,
            "Titulo",
            "Desc",
            TipoSolicitacao.REPARO,
            StatusSolicitacao.A_FAZER,
            null,
            modeloId,
            userId,
            null,
            NOW,
            NOW,
            null,
            null);
    final SolicitacaoJpaEntity jpa = SolicitacaoMapper.toJpa(s);

    assertEquals(id, jpa.getId());
    assertEquals("Titulo", jpa.getTitulo());
    assertEquals(TipoSolicitacao.REPARO, jpa.getTipo());
    assertEquals(StatusSolicitacao.A_FAZER, jpa.getStatus());
    assertNull(jpa.getPrioridade());
    assertNull(jpa.getConcluidaEm());
  }

  @Test
  void solicitacaoMapper_toDomain_comPrioridade() {
    final UUID id = UUID.randomUUID();
    final SolicitacaoJpaEntity e =
        new SolicitacaoJpaEntity(
            id,
            "T",
            "D",
            TipoSolicitacao.INSPECAO,
            StatusSolicitacao.EM_ANDAMENTO,
            PrioridadeSolicitacao.ALTA,
            UUID.randomUUID(),
            UUID.randomUUID(),
            null,
            NOW,
            NOW,
            null,
            null);
    final Solicitacao s = SolicitacaoMapper.toDomain(e);

    assertEquals(PrioridadeSolicitacao.ALTA, s.getPrioridade());
    assertEquals(StatusSolicitacao.EM_ANDAMENTO, s.getStatus());
  }

  // ──────── ModeloMapper ────────

  @Test
  void modeloMapper_toJpa_roundtrip() {
    final UUID id = UUID.randomUUID();
    final Modelo m =
        new Modelo(
            id,
            "MDL-001",
            1,
            "Desc modelo",
            null,
            "http://foto.url",
            NOW,
            "Bom estado",
            NOW,
            true,
            "CNC-01",
            false,
            NOW,
            NOW);
    final ModeloJpaEntity jpa = ModeloMapper.toJpa(m);

    assertEquals(id, jpa.getId());
    assertEquals("MDL-001", jpa.getCodigo());
    assertEquals(1, jpa.getVersao());
    assertEquals("http://foto.url", jpa.getFotoUrl());
    assertTrue(jpa.isAtivo());
    assertFalse(jpa.isTemPendenciaAberta());
  }

  @Test
  void modeloMapper_toDomain_roundtrip() {
    final UUID id = UUID.randomUUID();
    final ModeloJpaEntity e =
        new ModeloJpaEntity(
            id, "MDL-002", 2, "Desc", "Obs", null, null, null, null, false, "CNC-02", true, NOW,
            NOW);
    final Modelo m = ModeloMapper.toDomain(e);

    assertEquals(id, m.getId());
    assertEquals(2, m.getVersao());
    assertNull(m.getFotoUrl());
    assertFalse(m.isAtivo());
    assertTrue(m.isTemPendenciaAberta());
  }

  // ──────── EvidenciaMapper ────────

  @Test
  void evidenciaMapper_toJpa_roundtrip() {
    final UUID id = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final Evidencia ev =
        new Evidencia(id, "http://url", "image/jpeg", "foto.jpg", 1024, userId, NOW);
    final EvidenciaJpaEntity jpa = EvidenciaMapper.toJpa(ev);

    assertEquals(id, jpa.getId());
    assertEquals("http://url", jpa.getPublicUrl());
    assertEquals("image/jpeg", jpa.getMimeType());
    assertEquals(1024, jpa.getTamanhoBytes());
    assertEquals(userId, jpa.getEnviadaPorUsuarioId());
  }

  @Test
  void evidenciaMapper_toDomain_roundtrip() {
    final UUID id = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final EvidenciaJpaEntity e =
        new EvidenciaJpaEntity(id, "http://url2", "application/pdf", "doc.pdf", 2048, userId, NOW);
    final Evidencia ev = EvidenciaMapper.toDomain(e);

    assertEquals(id, ev.getId());
    assertEquals("application/pdf", ev.getMimeType());
    assertFalse(ev.isImagem());
  }

  // ──────── AtividadeSolicitacaoMapper ────────

  @Test
  void atividadeMapper_toJpa_roundtrip() {
    final UUID id = UUID.randomUUID();
    final UUID solId = UUID.randomUUID();
    final UUID autorId = UUID.randomUUID();
    final AtividadeSolicitacao a =
        new AtividadeSolicitacao(
            id,
            solId,
            TipoAtividadeSolicitacao.MUDANCA_STATUS,
            StatusSolicitacao.A_FAZER,
            StatusSolicitacao.EM_ANDAMENTO,
            null,
            autorId,
            NOW);
    final AtividadeSolicitacaoJpaEntity jpa = AtividadeSolicitacaoMapper.toJpa(a);

    assertEquals(id, jpa.getId());
    assertEquals(solId, jpa.getSolicitacaoId());
    assertEquals(TipoAtividadeSolicitacao.MUDANCA_STATUS, jpa.getTipo());
    assertEquals(StatusSolicitacao.A_FAZER, jpa.getDeStatus());
    assertEquals(StatusSolicitacao.EM_ANDAMENTO, jpa.getParaStatus());
  }

  @Test
  void atividadeMapper_toDomain_roundtrip() {
    final UUID id = UUID.randomUUID();
    final UUID solId = UUID.randomUUID();
    final UUID autorId = UUID.randomUUID();
    final AtividadeSolicitacaoJpaEntity e =
        new AtividadeSolicitacaoJpaEntity(
            id,
            solId,
            TipoAtividadeSolicitacao.COMENTARIO,
            null,
            null,
            "Bom trabalho",
            autorId,
            NOW);
    final AtividadeSolicitacao a = AtividadeSolicitacaoMapper.toDomain(e);

    assertEquals(TipoAtividadeSolicitacao.COMENTARIO, a.getTipo());
    assertEquals("Bom trabalho", a.getComentario());
    assertNull(a.getDeStatus());
  }

  // ──────── SolicitacaoAtribuicaoMapper ────────

  @Test
  void atribuicaoMapper_toJpa_roundtrip() {
    final UUID id = UUID.randomUUID();
    final UUID solId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final UUID gestorId = UUID.randomUUID();
    final SolicitacaoAtribuicao a =
        new SolicitacaoAtribuicao(id, solId, userId, gestorId, NOW, null);
    final SolicitacaoAtribuicaoJpaEntity jpa = SolicitacaoAtribuicaoMapper.toJpa(a);

    assertEquals(id, jpa.getId());
    assertEquals(solId, jpa.getSolicitacaoId());
    assertEquals(userId, jpa.getUsuarioId());
    assertEquals(gestorId, jpa.getAtribuidoPorUsuarioId());
    assertNull(jpa.getRemovidoEm());
  }

  @Test
  void atribuicaoMapper_toDomain_comRemovidoEm() {
    final UUID id = UUID.randomUUID();
    final Instant removido = Instant.parse("2025-06-01T00:00:00Z");
    final SolicitacaoAtribuicaoJpaEntity e =
        new SolicitacaoAtribuicaoJpaEntity(
            id, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), NOW, removido);
    final SolicitacaoAtribuicao a = SolicitacaoAtribuicaoMapper.toDomain(e);

    assertEquals(removido, a.getRemovidoEm());
  }

  // ──────── EventoModeloMapper ────────

  @Test
  void eventoModeloMapper_toJpa_roundtrip() {
    final UUID id = UUID.randomUUID();
    final UUID modeloId = UUID.randomUUID();
    final EventoModelo e =
        new EventoModelo(
            id,
            modeloId,
            TipoEventoModelo.MANUTENCAO,
            "Titulo",
            "Desc",
            "OK",
            true,
            UUID.randomUUID(),
            null,
            NOW);
    final EventoModeloJpaEntity jpa = EventoModeloMapper.toJpa(e);

    assertEquals(id, jpa.getId());
    assertEquals(modeloId, jpa.getModeloId());
    assertEquals(TipoEventoModelo.MANUTENCAO, jpa.getTipo());
    assertTrue(jpa.isDefineFotoCapa());
    assertNull(jpa.getSolicitacaoRelacionadaId());
  }

  @Test
  void eventoModeloMapper_toDomain_roundtrip() {
    final UUID id = UUID.randomUUID();
    final UUID solId = UUID.randomUUID();
    final EventoModeloJpaEntity e =
        new EventoModeloJpaEntity(
            id,
            UUID.randomUUID(),
            TipoEventoModelo.INSPECAO,
            "T",
            "D",
            "Estado",
            false,
            UUID.randomUUID(),
            solId,
            NOW);
    final EventoModelo ev = EventoModeloMapper.toDomain(e);

    assertEquals(TipoEventoModelo.INSPECAO, ev.getTipo());
    assertFalse(ev.isDefineFotoCapa());
    assertEquals(solId, ev.getSolicitacaoRelacionadaId());
  }
}
