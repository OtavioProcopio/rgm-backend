package com.rgm.api.adapter.out.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.rgm.api.adapter.out.persistence.ModeloRepositoryAdapter;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.ports.repositories.ContagensDeModelos;
import com.rgm.api.core.domain.ports.repositories.FiltrosDeModelos;
import com.rgm.api.core.domain.ports.repositories.OrdemDeModelos;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * O catálogo de modelos no PostgreSQL real: filtros somados, busca sem acento, ordem, contagens
 * facetadas e posição. A regra vive no SQL, então só o banco de verdade a prova.
 */
@Testcontainers
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ModeloCatalogoPostgresTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("rgm_test");

  @Autowired private ModeloJpaRepository jpa;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private EntityManagerFactory emf;

  private ModeloRepositoryAdapter adapter;
  private Instant base;

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
    adapter = new ModeloRepositoryAdapter(jpa);
    base = Instant.parse("2026-01-01T10:00:00Z");
    jdbc.update("DELETE FROM modelos");
    jdbc.update("DELETE FROM tipos_modelo");
  }

  private UUID modelo(
      final String codigo,
      final int versao,
      final String descricao,
      final String maquina,
      final String tipo,
      final boolean ativo,
      final int criadoHaMinutos) {
    final UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO modelos (id, codigo, versao, descricao, maquina, tipo, ativo, criado_em) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
        id,
        codigo,
        versao,
        descricao,
        maquina,
        tipo,
        ativo,
        java.sql.Timestamp.from(base.minusSeconds(criadoHaMinutos * 60L)));
    return id;
  }

  private static FiltrosDeModelos filtros(
      final Boolean ativo,
      final String maquina,
      final boolean exata,
      final String tipo,
      final String q) {
    return new FiltrosDeModelos(ativo, null, maquina, exata, null, tipo, q);
  }

  private static final FiltrosDeModelos TODOS = filtros(null, null, false, null, null);

  private List<String> codigos(final PageResult<Modelo> pagina) {
    return pagina.content().stream().map(m -> m.getCodigo() + "/v" + m.getVersao()).toList();
  }

  // ── filtros ──────────────────────────────────────────────────────────────

  @Test
  void shouldFilterByTipoIgnoringCase() {
    // Arrange
    modelo("A-1", 1, "Resina um", "FBOX", "RESINA", true, 1);
    modelo("A-2", 1, "Coquilha", "FBOX", "COQUILHA_ACO", true, 2);

    // Act
    final var pagina =
        adapter.buscarCatalogo(
            filtros(null, null, false, "resina", null), OrdemDeModelos.CODIGO, 0, 20);

    // Assert
    assertThat(codigos(pagina)).containsExactly("A-1/v1");
  }

  @Test
  void shouldSearchCodigoOrDescricaoWithoutCaseOrAccent() {
    // Arrange
    modelo("FB-1001", 1, "Carcaça de bomba", "FBOX", "RESINA", true, 1);
    modelo("RS-4001", 1, "Ponteira de válvula", "FBOX", "RESINA", true, 2);

    // Act + Assert
    assertThat(codigos(busca("CARCACA"))).containsExactly("FB-1001/v1");
    assertThat(codigos(busca("rs-40"))).containsExactly("RS-4001/v1");
    assertThat(codigos(busca("valvula"))).containsExactly("RS-4001/v1");
    assertThat(codigos(busca("zzz"))).isEmpty();
  }

  private PageResult<Modelo> busca(final String q) {
    return adapter.buscarCatalogo(
        filtros(null, null, false, null, q), OrdemDeModelos.CODIGO, 0, 20);
  }

  @Test
  void shouldTreatPercentAndUnderscoreInTheSearchAsPlainText() {
    // Arrange
    modelo("A-1", 1, "Peça 100% aço", "FBOX", "RESINA", true, 1);
    modelo("A-2", 1, "Peça comum", "FBOX", "RESINA", true, 2);
    modelo("A_3", 1, "Peça com sublinhado", "FBOX", "RESINA", true, 3);

    // Act + Assert
    assertThat(codigos(busca("100%"))).containsExactly("A-1/v1");
    assertThat(codigos(busca("a_3"))).containsExactly("A_3/v1");
  }

  @Test
  void shouldMatchMaquinaExactlyOnlyWhenAskedTo() {
    // Arrange
    modelo("A-1", 1, "d", "LINHA 2", "RESINA", true, 1);
    modelo("A-2", 1, "d", "LINHA 20", "RESINA", true, 2);

    // Act
    final var exata =
        adapter.buscarCatalogo(
            filtros(null, "linha 2", true, null, null), OrdemDeModelos.CODIGO, 0, 20);
    final var contem =
        adapter.buscarCatalogo(
            filtros(null, "LINHA 2", false, null, null), OrdemDeModelos.CODIGO, 0, 20);

    // Assert
    assertThat(codigos(exata)).containsExactly("A-1/v1");
    assertThat(codigos(contem)).containsExactly("A-1/v1", "A-2/v1");
  }

  @Test
  void shouldAddUpAllTheFilters() {
    // Arrange
    modelo("A-1", 1, "Carcaça", "FBOX", "RESINA", true, 1);
    modelo("A-2", 1, "Carcaça", "FBOX", "RESINA", false, 2);
    modelo("A-3", 1, "Carcaça", "LINHA 3", "RESINA", true, 3);
    modelo("A-4", 1, "Carcaça", "FBOX", "COQUILHA_ACO", true, 4);
    modelo("A-5", 1, "Outra", "FBOX", "RESINA", true, 5);

    // Act
    final var pagina =
        adapter.buscarCatalogo(
            filtros(true, "FBOX", true, "RESINA", "carcaca"), OrdemDeModelos.CODIGO, 0, 20);

    // Assert
    assertThat(codigos(pagina)).containsExactly("A-1/v1");
  }

  // ── ordem e paginação ───────────────────────────────────────────────────

  @Test
  void shouldOrderByCodigoThenHigherVersionFirst() {
    // Arrange
    modelo("B-2", 1, "d", "FBOX", "RESINA", true, 1);
    modelo("A-1", 1, "d", "FBOX", "RESINA", true, 2);
    modelo("A-1", 2, "d", "LINHA 2", "RESINA", true, 3);

    // Act
    final var pagina = adapter.buscarCatalogo(TODOS, OrdemDeModelos.CODIGO, 0, 20);

    // Assert
    assertThat(codigos(pagina)).containsExactly("A-1/v2", "A-1/v1", "B-2/v1");
  }

  @Test
  void shouldOrderByCriacaoMostRecentFirstWhenAsked() {
    // Arrange
    modelo("A-1", 1, "d", "FBOX", "RESINA", true, 30);
    modelo("B-2", 1, "d", "FBOX", "RESINA", true, 10);
    modelo("C-3", 1, "d", "FBOX", "RESINA", true, 20);

    // Act
    final var pagina = adapter.buscarCatalogo(TODOS, OrdemDeModelos.CRIACAO, 0, 20);

    // Assert
    assertThat(codigos(pagina)).containsExactly("B-2/v1", "C-3/v1", "A-1/v1");
  }

  @Test
  void shouldPageTheFilteredCatalogAndReportTheFilteredTotal() {
    // Arrange
    for (int i = 0; i < 45; i++)
      modelo(String.format("R-%03d", i), 1, "d", "FBOX", "RESINA", true, i);
    for (int i = 0; i < 10; i++)
      modelo(String.format("C-%03d", i), 1, "d", "FBOX", "COQUILHA_ACO", true, i);

    // Act
    final var terceira =
        adapter.buscarCatalogo(
            filtros(null, null, false, "RESINA", null), OrdemDeModelos.CODIGO, 2, 20);

    // Assert
    assertThat(terceira.content()).hasSize(5);
    assertThat(terceira.totalElements()).isEqualTo(45);
    assertThat(terceira.totalPages()).isEqualTo(3);
  }

  // ── contagens ───────────────────────────────────────────────────────────

  private void catalogoDeContagens() {
    modelo("A-1", 1, "Carcaça", "FBOX", "RESINA", true, 1);
    modelo("A-2", 1, "Carcaça", "FBOX", "RESINA", true, 2);
    modelo("A-3", 1, "Carcaça", "FBOX", "COQUILHA_ACO", true, 3);
    modelo("A-4", 1, "Carcaça", "LINHA 3", "RESINA", true, 4);
    modelo("A-5", 1, "Outra", "LINHA 3", "COQUILHA_ACO", true, 5);
    modelo("A-6", 1, "Inativa", "LINHA 3", "RESINA", false, 6);
    jdbc.update(
        "INSERT INTO modelos (id, codigo, versao, descricao, maquina, ativo) VALUES (?, 'S-1', 1, 'Sem tipo', 'FBOX', true)",
        UUID.randomUUID());
  }

  @Test
  void shouldCountTiposIgnoringTheTipoFilterAndMaquinasIgnoringTheMaquinaFilter() {
    // Arrange
    catalogoDeContagens();

    // Act: máquina FBOX exata e tipo RESINA escolhidos
    final ContagensDeModelos c = adapter.contar(filtros(true, "FBOX", true, "RESINA", null));

    // Assert
    assertThat(c.total()).isEqualTo(2);
    assertThat(c.porTipo())
        .extracting(ContagensDeModelos.Quantidade::valor, ContagensDeModelos.Quantidade::total)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("RESINA", 2L),
            org.assertj.core.groups.Tuple.tuple("COQUILHA_ACO", 1L));
    assertThat(c.porMaquina())
        .extracting(ContagensDeModelos.Quantidade::valor, ContagensDeModelos.Quantidade::total)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("FBOX", 2L),
            org.assertj.core.groups.Tuple.tuple("LINHA 3", 1L));
  }

  @Test
  void shouldLeaveOutTiposAndMaquinasWithNoModelAndModelsWithoutTipo() {
    // Arrange
    catalogoDeContagens();

    // Act: só ativos da LINHA 3
    final ContagensDeModelos c = adapter.contar(filtros(true, "LINHA 3", true, null, null));

    // Assert
    assertThat(c.porTipo())
        .extracting(ContagensDeModelos.Quantidade::valor)
        .containsExactlyInAnyOrder("RESINA", "COQUILHA_ACO");
    assertThat(c.porTipo()).extracting(ContagensDeModelos.Quantidade::total).containsOnly(1L);
    assertThat(c.total()).isEqualTo(2);
  }

  @Test
  void shouldOrderCountsByTotalThenAlphabetically() {
    // Arrange
    modelo("A-1", 1, "d", "ZETA", "TIPO_B", true, 1);
    modelo("A-2", 1, "d", "ALFA", "TIPO_A", true, 2);
    modelo("A-3", 1, "d", "ALFA", "TIPO_C", true, 3);
    modelo("A-4", 1, "d", "ALFA", "TIPO_C", true, 4);

    // Act
    final ContagensDeModelos c = adapter.contar(TODOS);

    // Assert
    assertThat(c.porTipo())
        .extracting(ContagensDeModelos.Quantidade::valor)
        .containsExactly("TIPO_C", "TIPO_A", "TIPO_B");
    assertThat(c.porMaquina())
        .extracting(ContagensDeModelos.Quantidade::valor)
        .containsExactly("ALFA", "ZETA");
  }

  @Test
  void shouldCountWithTheSameSearchTheListingUses() {
    // Arrange
    catalogoDeContagens();

    // Act
    final var q = filtros(true, null, false, null, "carcaca");
    final ContagensDeModelos c = adapter.contar(q);
    final var lista = adapter.buscarCatalogo(q, OrdemDeModelos.CODIGO, 0, 50);

    // Assert
    assertThat(c.total()).isEqualTo(lista.totalElements()).isEqualTo(4);
  }

  @Test
  void shouldRunAtMostThreeQueriesToCountNoMatterHowManyModelsExist() {
    // Arrange
    for (int i = 0; i < 300; i++)
      modelo(String.format("M-%04d", i), 1, "d", "M" + (i % 5), "T" + (i % 7), true, i);
    final var stats = emf.unwrap(SessionFactory.class).getStatistics();
    stats.clear();

    // Act
    adapter.contar(filtros(true, "M1", true, "T2", "m-0"));

    // Assert
    assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(3);
  }

  // ── posição ─────────────────────────────────────────────────────────────

  @Test
  void shouldGiveThePositionInTheFilteredListAndMatchTheListingPageAndRow() {
    // Arrange
    final List<UUID> ids = new ArrayList<>();
    for (int i = 0; i < 45; i++)
      ids.add(modelo(String.format("R-%03d", i), 1, "d", "FBOX", "RESINA", true, i));
    for (int i = 0; i < 10; i++)
      modelo(String.format("A-%03d", i), 1, "d", "FBOX", "COQUILHA_ACO", true, i);
    final var f = filtros(null, null, false, "RESINA", null);

    // Act + Assert: o 33º está na página 1, linha 12; e vale para todos
    final var posicao33 = adapter.posicaoDe(ids.get(32), f);
    assertThat(posicao33.posicao()).isEqualTo(33);
    assertThat(posicao33.total()).isEqualTo(45);
    for (final UUID id : ids) {
      final int p = adapter.posicaoDe(id, f).posicao();
      final var pagina = adapter.buscarCatalogo(f, OrdemDeModelos.CODIGO, (p - 1) / 20, 20);
      assertThat(pagina.content().get((p - 1) % 20).getId()).isEqualTo(id);
    }
  }

  @Test
  void shouldPlaceTheHigherVersionBeforeTheLowerOneOfTheSameCodigo() {
    // Arrange
    final UUID v1 = modelo("A-1", 1, "d", "FBOX", "RESINA", true, 1);
    final UUID v2 = modelo("A-1", 2, "d", "FBOX", "RESINA", true, 2);

    // Act + Assert
    assertThat(adapter.posicaoDe(v2, TODOS).posicao()).isEqualTo(1);
    assertThat(adapter.posicaoDe(v1, TODOS).posicao()).isEqualTo(2);
  }

  @Test
  void shouldBreakTiesByIdWhenCodigoAndVersaoRepeatInAnotherMachine() {
    // Arrange: mesmo código e versão em máquinas diferentes
    final UUID a = modelo("A-1", 1, "d", "FBOX", "RESINA", true, 1);
    final UUID b = modelo("A-1", 1, "d", "LINHA 2", "RESINA", true, 2);

    // Act
    final int pa = adapter.posicaoDe(a, TODOS).posicao();
    final int pb = adapter.posicaoDe(b, TODOS).posicao();
    final var lista = adapter.buscarCatalogo(TODOS, OrdemDeModelos.CODIGO, 0, 10).content();

    // Assert
    assertThat(pa).isNotEqualTo(pb);
    assertThat(lista.get(pa - 1).getId()).isEqualTo(a);
    assertThat(lista.get(pb - 1).getId()).isEqualTo(b);
  }

  @Test
  void shouldReturnNullPositionAndTheTotalWhenTheModelIsOutsideTheFilter() {
    // Arrange
    modelo("A-1", 1, "d", "FBOX", "RESINA", true, 1);
    final UUID fora = modelo("A-2", 1, "d", "LINHA 3", "RESINA", true, 2);

    // Act
    final var posicao = adapter.posicaoDe(fora, filtros(null, "FBOX", true, null, null));

    // Assert
    assertThat(posicao.posicao()).isNull();
    assertThat(posicao.total()).isEqualTo(1);
  }
}
