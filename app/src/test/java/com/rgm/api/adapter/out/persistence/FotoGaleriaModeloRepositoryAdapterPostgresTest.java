package com.rgm.api.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.rgm.api.core.domain.model.aggregates.FotoGaleriaModelo;
import com.rgm.api.core.domain.ports.repositories.FotoGaleriaModeloRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
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
 * Prova a troca de capa da galeria em PostgreSQL real e <b>sem</b> a transacao que o {@code
 * DataJpaTest} abre por padrao: e a ausencia de transacao no caminho de producao que o mock do
 * repositorio escondia (issue #115).
 */
@Testcontainers
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(FotoGaleriaModeloRepositoryAdapter.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class FotoGaleriaModeloRepositoryAdapterPostgresTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("rgm_test");

  @Autowired private FotoGaleriaModeloRepository adapter;
  @Autowired private JdbcTemplate jdbc;

  private UUID modeloId;
  private UUID outroModeloId;

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
    modeloId = criarModelo();
    outroModeloId = criarModelo();
  }

  @AfterEach
  void limpar() {
    jdbc.update(
        "DELETE FROM fotos_galeria_modelo WHERE modelo_id IN (?, ?)", modeloId, outroModeloId);
    jdbc.update("DELETE FROM modelos WHERE id IN (?, ?)", modeloId, outroModeloId);
  }

  private UUID criarModelo() {
    final UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO modelos (id, codigo, descricao, maquina) VALUES (?, ?, 'Modelo', 'FBOX')",
        id,
        "M-" + id.toString().substring(0, 8));
    return id;
  }

  private FotoGaleriaModelo gravar(
      final UUID modelo, final String identificacao, final boolean principal) {
    return adapter.save(
        FotoGaleriaModelo.criar(
            modelo,
            "http://minio/" + identificacao + ".jpg",
            identificacao,
            principal,
            null,
            Instant.now()));
  }

  private List<UUID> principaisDo(final UUID modelo) {
    return jdbc.queryForList(
        "SELECT id FROM fotos_galeria_modelo WHERE modelo_id = ? AND principal = true",
        UUID.class,
        modelo);
  }

  @Test
  void shouldDeixarSoANovaComoCapaWhenSalvarComoPrincipal() {
    // Arrange
    final FotoGaleriaModelo primeira = gravar(modeloId, "primeira", true);
    final FotoGaleriaModelo segunda = gravar(modeloId, "segunda", false);

    // Act
    final FotoGaleriaModelo resultado = adapter.salvarComoPrincipal(segunda.comPrincipal(true));

    // Assert
    assertEquals(segunda.getId(), resultado.getId());
    assertEquals(List.of(segunda.getId()), principaisDo(modeloId));
    assertEquals(2, adapter.findByModeloId(modeloId).size());
    assertEquals(primeira.getId(), adapter.findById(primeira.getId()).orElseThrow().getId());
  }

  @Test
  void shouldNaoMexerNaCapaDeOutroModeloWhenSalvarComoPrincipal() {
    // Arrange
    final FotoGaleriaModelo capaDoOutro = gravar(outroModeloId, "capa-outro", true);
    final FotoGaleriaModelo foto = gravar(modeloId, "foto", false);

    // Act
    adapter.salvarComoPrincipal(foto.comPrincipal(true));

    // Assert
    assertEquals(List.of(capaDoOutro.getId()), principaisDo(outroModeloId));
    assertEquals(List.of(foto.getId()), principaisDo(modeloId));
  }

  @Test
  void shouldManterACapaAntigaWhenGravarANovaFalha() {
    // Arrange
    final FotoGaleriaModelo primeira = gravar(modeloId, "primeira", true);
    final FotoGaleriaModelo segunda = gravar(modeloId, "segunda", false);
    final FotoGaleriaModelo invalida = segunda.comIdentificacao("x".repeat(300)).comPrincipal(true);

    // Act
    assertThrows(DataAccessException.class, () -> adapter.salvarComoPrincipal(invalida));

    // Assert
    assertEquals(List.of(primeira.getId()), principaisDo(modeloId));
  }
}
