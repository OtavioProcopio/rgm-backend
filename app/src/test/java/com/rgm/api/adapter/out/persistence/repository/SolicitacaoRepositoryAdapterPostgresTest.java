package com.rgm.api.adapter.out.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.rgm.api.adapter.out.persistence.SolicitacaoRepositoryAdapter;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * A instrucao da solicitacao no PostgreSQL real, pelo adaptador de dominio: gravacao fiel do texto
 * e o lock otimista que impede duas correcoes simultaneas de se sobreporem. Cada operacao roda na
 * propria transacao (como em producao), por isso o teste limpa o que cria.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SolicitacaoRepositoryAdapterPostgresTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("rgm_test");

  @Autowired private SolicitacaoJpaRepository jpa;
  @Autowired private JdbcTemplate jdbc;

  private SolicitacaoRepositoryAdapter adapter;
  private UUID usuarioId;
  private UUID modeloId;

  @DynamicPropertySource
  static void configurar(final DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    registry.add("spring.flyway.enabled", () -> "true");
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
  }

  @BeforeEach
  void setUp() {
    adapter = new SolicitacaoRepositoryAdapter(jpa);
    usuarioId = UUID.randomUUID();
    modeloId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO usuarios (id, nome, perfil) VALUES (?, 'Gestor', 'GESTOR')", usuarioId);
    jdbc.update(
        "INSERT INTO modelos (id, codigo, descricao, maquina) VALUES (?, ?, 'Modelo', 'FBOX')",
        modeloId,
        "M-" + modeloId.toString().substring(0, 8));
  }

  @AfterEach
  void limpar() {
    jdbc.update("DELETE FROM solicitacoes WHERE aberta_por_usuario_id = ?", usuarioId);
    jdbc.update("DELETE FROM modelos WHERE id = ?", modeloId);
    jdbc.update("DELETE FROM usuarios WHERE id = ?", usuarioId);
  }

  private Solicitacao emAndamento(final String instrucao) {
    final Instant agora = Instant.now();
    return Solicitacao.builder()
        .id(UUID.randomUUID())
        .titulo("Trocar peca")
        .descricao("Descricao")
        .tipo(TipoSolicitacao.REPARO)
        .status(StatusSolicitacao.EM_ANDAMENTO)
        .prioridade(PrioridadeSolicitacao.ALTA)
        .modeloId(modeloId)
        .abertaPorUsuarioId(usuarioId)
        .instrucao(instrucao)
        .criadaEm(agora)
        .atualizadaEm(agora)
        .build();
  }

  @Test
  void shouldStoreAndReadBackTheInstrucaoExactlyWithAccentsLineBreaksAndTheLimit() {
    // Arrange
    final String instrucao =
        "Trocar o gabarito\nConferir o furo 3 — ação urgente\n" + "x".repeat(1900);
    final Solicitacao nova = emAndamento(instrucao);

    // Act
    adapter.save(nova);

    // Assert
    assertEquals(
        instrucao,
        jdbc.queryForObject(
            "SELECT instrucao FROM solicitacoes WHERE id = ?", String.class, nova.getId()));
    assertEquals(instrucao, adapter.findById(nova.getId()).orElseThrow().getInstrucao());
  }

  @Test
  void shouldStoreNullWhenTheSolicitacaoHasNoInstrucao() {
    // Arrange
    final Solicitacao semInstrucao = emAndamento(null);

    // Act
    adapter.save(semInstrucao);

    // Assert
    assertNull(adapter.findById(semInstrucao.getId()).orElseThrow().getInstrucao());
  }

  @Test
  void shouldRejectTheSecondEditWhenBothStartedFromTheSameVersion() {
    // Arrange: dois gestores abrem a mesma solicitacao na mesma versao
    final Solicitacao salva = adapter.save(emAndamento("Original"));
    final Solicitacao doPrimeiro = adapter.findById(salva.getId()).orElseThrow();
    final Solicitacao doSegundo = adapter.findById(salva.getId()).orElseThrow();
    final Instant agora = Instant.now();

    // Act
    adapter.save(doPrimeiro.editarInstrucao("Do primeiro gestor", agora));

    // Assert
    assertThrows(
        OptimisticLockingFailureException.class,
        () -> adapter.save(doSegundo.editarInstrucao("Do segundo gestor", agora)));
    assertEquals(
        "Do primeiro gestor", adapter.findById(salva.getId()).orElseThrow().getInstrucao());
  }
}
