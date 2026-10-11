package com.rgm.api.adapter.out.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.rgm.api.adapter.out.persistence.entity.UsuarioJpaEntity;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UsuarioJpaRepositoryPostgresTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("rgm_test");

  private static final List<String> ATRIBUIVEIS = List.of("OPERADOR", "GESTOR");

  @Autowired private UsuarioJpaRepository repository;
  @Autowired private JdbcTemplate jdbc;

  @DynamicPropertySource
  static void configurar(final DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    registry.add("spring.flyway.enabled", () -> "true");
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
  }

  private UUID criar(final String nome, final String perfil, final boolean ativo) {
    final UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO usuarios (id, nome, perfil, ativo) VALUES (?, ?, ?, ?)",
        id,
        nome,
        perfil,
        ativo);
    return id;
  }

  private static List<UUID> idsDe(final List<UsuarioJpaEntity> usuarios) {
    return usuarios.stream().map(UsuarioJpaEntity::getId).toList();
  }

  @Test
  void shouldReachUserBeyondFirstHundredWhenSearchingByNameWithoutAccent() {
    // Arrange
    for (int i = 0; i < 120; i++) {
      criar(String.format("Operador %03d", i), "OPERADOR", true);
    }
    final UUID zelia = criar("Zélia Souza", "OPERADOR", true);

    // Act
    final var pagina = repository.buscarAtribuiveis(ATRIBUIVEIS, "ZELIA", PageRequest.of(0, 20));

    // Assert
    assertEquals(List.of(zelia), idsDe(pagina.getContent()));
  }

  @Test
  void shouldListOnlyActiveOperatorsAndManagersOrderedByName() {
    // Arrange
    final UUID beto = criar("Beto", "OPERADOR", true);
    final UUID ana = criar("Ana", "GESTOR", true);
    criar("Carlos", "ADMINISTRADOR", true);
    criar("Diana", "EXTERNO", true);
    criar("Eva", "OPERADOR", false);

    // Act
    final var pagina = repository.buscarAtribuiveis(ATRIBUIVEIS, null, PageRequest.of(0, 20));

    // Assert
    assertEquals(List.of(ana, beto), idsDe(pagina.getContent()));
    assertEquals(2, pagina.getTotalElements());
  }

  @Test
  void shouldRespectPageAndSize() {
    // Arrange
    criar("Aaa", "OPERADOR", true);
    criar("Bbb", "OPERADOR", true);
    final UUID c = criar("Ccc", "OPERADOR", true);

    // Act
    final var segunda = repository.buscarAtribuiveis(ATRIBUIVEIS, null, PageRequest.of(1, 2));

    // Assert
    assertEquals(List.of(c), idsDe(segunda.getContent()));
    assertEquals(3, segunda.getTotalElements());
  }
}
