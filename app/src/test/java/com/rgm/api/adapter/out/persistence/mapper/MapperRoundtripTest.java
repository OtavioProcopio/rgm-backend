package com.rgm.api.adapter.out.persistence.mapper;

import static com.rgm.api.support.SolicitacaoTestBuilder.umaSolicitacao;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.rgm.api.core.domain.model.aggregates.EventoModelo;
import com.rgm.api.core.domain.model.aggregates.Evidencia;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.model.entities.SolicitacaoAtribuicao;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoAtividadeSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoEventoModelo;
import com.rgm.api.core.domain.model.enums.TipoEvidencia;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Full-field round-trip tests guarding against positional-constructor field-swap bugs in the
 * persistence mappers. Every field is asserted through domain -> jpa -> domain so a misplaced
 * argument cannot survive unnoticed.
 */
class MapperRoundtripTest {

  private static final Instant T1 = Instant.parse("2025-01-01T10:00:00Z");
  private static final Instant T2 = Instant.parse("2025-02-02T11:11:11Z");
  private static final Instant T3 = Instant.parse("2025-03-03T12:22:33Z");
  private static final Instant T4 = Instant.parse("2025-04-04T13:33:44Z");

  // ──────── UsuarioMapper ────────

  @Test
  void shouldKeepEveryUserFieldWhenMappedToJpaAndBack() {
    // Arrange
    final Usuario original =
        new Usuario(
            UUID.randomUUID(),
            "Carol",
            "carol@x.com",
            "hashXYZ",
            PerfilUsuario.ADMINISTRADOR,
            true,
            T1,
            T2,
            5);

    // Act
    final Usuario result = UsuarioMapper.toDomain(UsuarioMapper.toJpa(original));

    // Assert
    assertEquals(
        List.of(
            original.getId(),
            original.getNome(),
            original.getEmail(),
            original.getSenhaHash(),
            original.getPerfil(),
            original.isAtivo(),
            original.getCriadoEm(),
            original.getAtualizadoEm(),
            original.getVersaoCredencial()),
        List.of(
            result.getId(),
            result.getNome(),
            result.getEmail(),
            result.getSenhaHash(),
            result.getPerfil(),
            result.isAtivo(),
            result.getCriadoEm(),
            result.getAtualizadoEm(),
            result.getVersaoCredencial()));
  }

  // ──────── SolicitacaoMapper ────────

  @Test
  void solicitacaoMapper_fullRoundtrip_concluida() {
    final UUID id = UUID.randomUUID();
    final UUID modeloId = UUID.randomUUID();
    final UUID abertaPor = UUID.randomUUID();
    final Solicitacao original =
        Solicitacao.builder()
            .id(id)
            .titulo("Trocar rolamento")
            .descricao("Descricao detalhada")
            .tipo(TipoSolicitacao.REPARO)
            .status(StatusSolicitacao.CONCLUIDA)
            .prioridade(PrioridadeSolicitacao.ALTA)
            .modeloId(modeloId)
            .abertaPorUsuarioId(abertaPor)
            .comentarioFinal("Comentario final")
            .criadaEm(T1)
            .atualizadaEm(T2)
            .concluidaEm(T3)
            .build();

    final Solicitacao result = SolicitacaoMapper.toDomain(SolicitacaoMapper.toJpa(original));

    assertEquals(id, result.getId());
    assertEquals("Trocar rolamento", result.getTitulo());
    assertEquals("Descricao detalhada", result.getDescricao());
    assertEquals(TipoSolicitacao.REPARO, result.getTipo());
    assertEquals(StatusSolicitacao.CONCLUIDA, result.getStatus());
    assertEquals(PrioridadeSolicitacao.ALTA, result.getPrioridade());
    assertEquals(modeloId, result.getModeloId());
    assertEquals(abertaPor, result.getAbertaPorUsuarioId());
    assertEquals("Comentario final", result.getComentarioFinal());
    assertEquals(T1, result.getCriadaEm());
    assertEquals(T2, result.getAtualizadaEm());
    assertEquals(T3, result.getConcluidaEm());
    assertNull(result.getCanceladaEm());
  }

  @Test
  void solicitacaoMapper_fullRoundtrip_cancelada() {
    final Solicitacao original =
        Solicitacao.builder()
            .id(UUID.randomUUID())
            .titulo("Cancelada")
            .descricao("Desc")
            .tipo(TipoSolicitacao.REPARO)
            .status(StatusSolicitacao.CANCELADA)
            .prioridade(PrioridadeSolicitacao.BAIXA)
            .modeloId(UUID.randomUUID())
            .abertaPorUsuarioId(UUID.randomUUID())
            .comentarioFinal("Motivo do cancelamento")
            .criadaEm(T1)
            .atualizadaEm(T2)
            .canceladaEm(T4)
            .build();

    final Solicitacao result = SolicitacaoMapper.toDomain(SolicitacaoMapper.toJpa(original));

    assertEquals(StatusSolicitacao.CANCELADA, result.getStatus());
    assertEquals("Motivo do cancelamento", result.getComentarioFinal());
    assertNull(result.getConcluidaEm());
    assertEquals(T4, result.getCanceladaEm());
  }

  @Test
  void solicitacaoMapper_fullRoundtrip_camposNulos() {
    final Solicitacao original =
        Solicitacao.builder()
            .id(UUID.randomUUID())
            .titulo("T")
            .descricao("D")
            .tipo(TipoSolicitacao.INSPECAO)
            .status(StatusSolicitacao.A_FAZER)
            .modeloId(UUID.randomUUID())
            .abertaPorUsuarioId(UUID.randomUUID())
            .criadaEm(T1)
            .atualizadaEm(T1)
            .build();

    final Solicitacao result = SolicitacaoMapper.toDomain(SolicitacaoMapper.toJpa(original));

    assertNull(result.getPrioridade());
    assertNull(result.getComentarioFinal());
    assertNull(result.getConcluidaEm());
    assertNull(result.getCanceladaEm());
  }

  // ──────── ModeloMapper ────────

  @Test
  void shouldKeepInstrucaoWithAccentsLineBreaksAndTheLimitWhenMappedToJpaAndBack() {
    // Arrange
    final String instrucao =
        "Trocar o gabarito\nConferir o furo 3 — ação urgente\n" + "x".repeat(1900);
    final Solicitacao original =
        Solicitacao.builder()
            .id(UUID.randomUUID())
            .titulo("Trocar rolamento")
            .descricao("Descricao")
            .tipo(TipoSolicitacao.REPARO)
            .status(StatusSolicitacao.EM_ANDAMENTO)
            .prioridade(PrioridadeSolicitacao.ALTA)
            .modeloId(UUID.randomUUID())
            .abertaPorUsuarioId(UUID.randomUUID())
            .instrucao(instrucao)
            .criadaEm(T1)
            .atualizadaEm(T2)
            .build();

    // Act
    final var jpa = SolicitacaoMapper.toJpa(original);
    final Solicitacao result = SolicitacaoMapper.toDomain(jpa);

    // Assert
    assertEquals(instrucao, jpa.getInstrucao());
    assertEquals(instrucao, result.getInstrucao());
  }

  @Test
  void shouldKeepNullInstrucaoWhenMappedToJpaAndBack() {
    // Arrange
    final Solicitacao original = umaSolicitacao().emAndamento().build();

    // Act
    final Solicitacao result = SolicitacaoMapper.toDomain(SolicitacaoMapper.toJpa(original));

    // Assert
    assertNull(result.getInstrucao());
  }

  @Test
  void modeloMapper_fullRoundtrip() {
    final UUID id = UUID.randomUUID();
    final Modelo original =
        new Modelo(
            id,
            "MDL-100",
            3,
            "Descricao modelo",
            "Observacoes",
            "Estado atual",
            T2,
            true,
            "CNC-99",
            null,
            true,
            T3,
            T4);

    final Modelo result = ModeloMapper.toDomain(ModeloMapper.toJpa(original));

    assertEquals(id, result.getId());
    assertEquals("MDL-100", result.getCodigo());
    assertEquals(3, result.getVersao());
    assertEquals("Descricao modelo", result.getDescricao());
    assertEquals("Observacoes", result.getObservacoes());
    assertEquals("Estado atual", result.getEstadoAtualDescricao());
    assertEquals(T2, result.getEstadoAtualAtualizadoEm());
    assertEquals(true, result.isAtivo());
    assertEquals("CNC-99", result.getMaquina());
    assertEquals(true, result.isTemPendenciaAberta());
    assertEquals(T3, result.getCriadoEm());
    assertEquals(T4, result.getAtualizadoEm());
  }

  @Test
  void modeloMapper_fullRoundtrip_camposNulos() {
    final Modelo original =
        new Modelo(
            UUID.randomUUID(),
            "MDL-200",
            1,
            "Desc",
            null,
            null,
            null,
            false,
            "CNC-00",
            null,
            false,
            T1,
            T1);

    final Modelo result = ModeloMapper.toDomain(ModeloMapper.toJpa(original));

    assertNull(result.getObservacoes());
    assertNull(result.getEstadoAtualDescricao());
    assertNull(result.getEstadoAtualAtualizadoEm());
    assertEquals(false, result.isAtivo());
    assertEquals(false, result.isTemPendenciaAberta());
  }

  // ──────── EvidenciaMapper ────────

  @Test
  void evidenciaMapper_fullRoundtrip() {
    final UUID id = UUID.randomUUID();
    final UUID enviadaPor = UUID.randomUUID();
    final Evidencia original =
        new Evidencia(
            id,
            "http://url/img.png",
            "image/png",
            "img.png",
            4096,
            enviadaPor,
            T1,
            TipoEvidencia.GERAL,
            null);

    final Evidencia result = EvidenciaMapper.toDomain(EvidenciaMapper.toJpa(original));

    assertEquals(id, result.getId());
    assertEquals("http://url/img.png", result.getPublicUrl());
    assertEquals("image/png", result.getMimeType());
    assertEquals("img.png", result.getNomeArquivo());
    assertEquals(4096, result.getTamanhoBytes());
    assertEquals(enviadaPor, result.getEnviadaPorUsuarioId());
    assertEquals(T1, result.getCriadaEm());
  }

  // ──────── AtividadeSolicitacaoMapper ────────

  @Test
  void shouldKeepTheInstrucaoAlteradaTypeAndPreviousTextWhenMappedToJpaAndBack() {
    // Arrange
    final AtividadeSolicitacao original =
        AtividadeSolicitacao.instrucaoAlterada(
            UUID.randomUUID(), "Texto anterior\ncom acentuação", UUID.randomUUID(), T1);

    // Act
    final AtividadeSolicitacao result =
        AtividadeSolicitacaoMapper.toDomain(AtividadeSolicitacaoMapper.toJpa(original));

    // Assert
    assertEquals(TipoAtividadeSolicitacao.INSTRUCAO_ALTERADA, result.getTipo());
    assertEquals("Texto anterior\ncom acentuação", result.getComentario());
    assertEquals(original.getAutorUsuarioId(), result.getAutorUsuarioId());
  }

  @Test
  void atividadeMapper_fullRoundtrip() {
    final UUID id = UUID.randomUUID();
    final UUID solId = UUID.randomUUID();
    final UUID autorId = UUID.randomUUID();
    final AtividadeSolicitacao original =
        new AtividadeSolicitacao(
            id,
            solId,
            TipoAtividadeSolicitacao.MUDANCA_STATUS,
            StatusSolicitacao.EM_ANDAMENTO,
            StatusSolicitacao.EM_VALIDACAO,
            "Enviado para validacao",
            autorId,
            T1);

    final AtividadeSolicitacao result =
        AtividadeSolicitacaoMapper.toDomain(AtividadeSolicitacaoMapper.toJpa(original));

    assertEquals(id, result.getId());
    assertEquals(solId, result.getSolicitacaoId());
    assertEquals(TipoAtividadeSolicitacao.MUDANCA_STATUS, result.getTipo());
    assertEquals(StatusSolicitacao.EM_ANDAMENTO, result.getDeStatus());
    assertEquals(StatusSolicitacao.EM_VALIDACAO, result.getParaStatus());
    assertEquals("Enviado para validacao", result.getComentario());
    assertEquals(autorId, result.getAutorUsuarioId());
    assertEquals(T1, result.getCriadaEm());
  }

  // ──────── SolicitacaoAtribuicaoMapper ────────

  @Test
  void atribuicaoMapper_fullRoundtrip() {
    final UUID id = UUID.randomUUID();
    final UUID solId = UUID.randomUUID();
    final UUID usuarioId = UUID.randomUUID();
    final UUID atribuidoPor = UUID.randomUUID();
    final SolicitacaoAtribuicao original =
        new SolicitacaoAtribuicao(id, solId, usuarioId, atribuidoPor, T1, T2);

    final SolicitacaoAtribuicao result =
        SolicitacaoAtribuicaoMapper.toDomain(SolicitacaoAtribuicaoMapper.toJpa(original));

    assertEquals(id, result.getId());
    assertEquals(solId, result.getSolicitacaoId());
    assertEquals(usuarioId, result.getUsuarioId());
    assertEquals(atribuidoPor, result.getAtribuidoPorUsuarioId());
    assertEquals(T1, result.getAtribuidoEm());
    assertEquals(T2, result.getRemovidoEm());
  }

  // ──────── EventoModeloMapper ────────

  @Test
  void eventoModeloMapper_fullRoundtrip() {
    final UUID id = UUID.randomUUID();
    final UUID modeloId = UUID.randomUUID();
    final UUID executadoPor = UUID.randomUUID();
    final UUID solRelacionada = UUID.randomUUID();
    final EventoModelo original =
        new EventoModelo(
            id,
            modeloId,
            TipoEventoModelo.MANUTENCAO,
            "Titulo evento",
            "Descricao evento",
            "Estado descricao",
            executadoPor,
            solRelacionada,
            T1);

    final EventoModelo result = EventoModeloMapper.toDomain(EventoModeloMapper.toJpa(original));

    assertEquals(id, result.getId());
    assertEquals(modeloId, result.getModeloId());
    assertEquals(TipoEventoModelo.MANUTENCAO, result.getTipo());
    assertEquals("Titulo evento", result.getTitulo());
    assertEquals("Descricao evento", result.getDescricao());
    assertEquals("Estado descricao", result.getEstadoModeloDescricao());
    assertEquals(executadoPor, result.getExecutadoPorUsuarioId());
    assertEquals(solRelacionada, result.getSolicitacaoRelacionadaId());
    assertEquals(T1, result.getCriadoEm());
  }

  @Test
  void shouldKeepOptimisticLockVersionWhenSolicitacaoIsMappedToJpaAndBack() {
    // Arrange
    final Solicitacao original =
        Solicitacao.builder()
            .id(UUID.randomUUID())
            .titulo("T")
            .descricao("D")
            .tipo(TipoSolicitacao.REPARO)
            .status(StatusSolicitacao.EM_ANDAMENTO)
            .prioridade(PrioridadeSolicitacao.MEDIA)
            .modeloId(UUID.randomUUID())
            .abertaPorUsuarioId(UUID.randomUUID())
            .criadaEm(T1)
            .atualizadaEm(T1)
            .version(5L)
            .build();

    // Act
    final Solicitacao result = SolicitacaoMapper.toDomain(SolicitacaoMapper.toJpa(original));

    // Assert
    assertEquals(5L, result.getVersion());
  }
}
