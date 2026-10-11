package com.rgm.api.adapter.out.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.rgm.api.adapter.out.persistence.entity.SolicitacaoJpaEntity;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class SolicitacaoJpaRepositoryTest {

  private static final Instant BASE = Instant.parse("2026-09-01T12:00:00Z");

  @Autowired private SolicitacaoJpaRepository repository;

  private UUID modeloId;

  @BeforeEach
  void setUp() {
    repository.deleteAll();
    modeloId = UUID.randomUUID();
  }

  private void persistir(
      final UUID modelo,
      final StatusSolicitacao status,
      final Instant criadaEm,
      final Duration resolucao) {
    final SolicitacaoJpaEntity s = new SolicitacaoJpaEntity();
    s.setId(UUID.randomUUID());
    s.setTitulo("Trocar peca");
    s.setTipo(TipoSolicitacao.REPARO);
    s.setStatus(status);
    s.setModeloId(modelo);
    s.setAbertaPorUsuarioId(UUID.randomUUID());
    s.setCriadaEm(criadaEm);
    s.setAtualizadaEm(criadaEm);
    if (status == StatusSolicitacao.CONCLUIDA) {
      s.setConcluidaEm(criadaEm.plus(resolucao));
    }
    if (status == StatusSolicitacao.CANCELADA) {
      s.setCanceladaEm(criadaEm.plus(resolucao));
    }
    repository.saveAndFlush(s);
  }

  private void persistirHistoricoDoModelo() {
    persistir(modeloId, StatusSolicitacao.CONCLUIDA, BASE, Duration.ofDays(2));
    persistir(
        modeloId, StatusSolicitacao.CANCELADA, BASE.plus(Duration.ofDays(10)), Duration.ofDays(9));
    persistir(
        modeloId, StatusSolicitacao.CONCLUIDA, BASE.plus(Duration.ofDays(20)), Duration.ofDays(4));
    persistir(modeloId, StatusSolicitacao.A_FAZER, BASE.plus(Duration.ofDays(30)), null);
    persistir(modeloId, StatusSolicitacao.EM_ANDAMENTO, BASE.plus(Duration.ofDays(31)), null);
    persistir(modeloId, StatusSolicitacao.EM_VALIDACAO, BASE.plus(Duration.ofDays(32)), null);
    persistir(
        UUID.randomUUID(),
        StatusSolicitacao.CONCLUIDA,
        BASE.minus(Duration.ofDays(90)),
        Duration.ofDays(50));
  }

  @Test
  void shouldContarPorSituacaoSoAsSolicitacoesDoModeloWhenResumePorModelo() {
    // Arrange
    persistirHistoricoDoModelo();

    // Act
    final Object[] row = repository.resumirPorModelo(modeloId).get(0);

    // Assert
    assertEquals(6L, ((Number) row[0]).longValue());
    assertEquals(3L, ((Number) row[1]).longValue());
    assertEquals(2L, ((Number) row[2]).longValue());
    assertEquals(1L, ((Number) row[3]).longValue());
  }

  @Test
  void shouldDevolverPrimeiraEUltimaAberturaWhenResumePorModelo() {
    // Arrange
    persistirHistoricoDoModelo();

    // Act
    final Object[] row = repository.resumirPorModelo(modeloId).get(0);

    // Assert
    assertEquals(BASE, row[4]);
    assertEquals(BASE.plus(Duration.ofDays(32)), row[5]);
  }

  @Test
  void shouldCalcularAMediaDeResolucaoSoComAsConcluidasWhenResumePorModelo() {
    // Arrange
    persistirHistoricoDoModelo();
    final double esperado = Duration.ofDays(3).getSeconds();

    // Act
    final Object[] row = repository.resumirPorModelo(modeloId).get(0);

    // Assert
    assertEquals(esperado, ((Number) row[6]).doubleValue(), 0.001);
  }

  @Test
  void shouldDevolverContagensZeradasETemposVaziosWhenModeloNaoTemSolicitacao() {
    // Arrange
    persistir(UUID.randomUUID(), StatusSolicitacao.CONCLUIDA, BASE, Duration.ofDays(1));

    // Act
    final Object[] row = repository.resumirPorModelo(modeloId).get(0);

    // Assert
    assertEquals(0L, ((Number) row[0]).longValue());
    assertEquals(0L, ((Number) row[1]).longValue());
    assertNull(row[4]);
    assertNull(row[6]);
  }
}
