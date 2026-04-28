package com.rgm.api.core.application.usecases.evidencia;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Evidencia;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.EvidenciaRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoEvidenciaRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.services.StorageService;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnexarEvidenciaUseCaseTest {

  private SolicitacaoRepository solicitacaoRepository;
  private EvidenciaRepository evidenciaRepository;
  private SolicitacaoEvidenciaRepository solicitacaoEvidenciaRepository;
  private AtividadeSolicitacaoRepository atividadeRepository;
  private StorageService storageService;
  private AnexarEvidenciaUseCase useCase;

  @BeforeEach
  void setUp() {
    solicitacaoRepository = mock(SolicitacaoRepository.class);
    evidenciaRepository = mock(EvidenciaRepository.class);
    solicitacaoEvidenciaRepository = mock(SolicitacaoEvidenciaRepository.class);
    atividadeRepository = mock(AtividadeSolicitacaoRepository.class);
    storageService = mock(StorageService.class);
    useCase =
        new AnexarEvidenciaUseCase(
            solicitacaoRepository,
            evidenciaRepository,
            solicitacaoEvidenciaRepository,
            atividadeRepository,
            storageService);
  }

  private Solicitacao criarSolicitacao(final StatusSolicitacao status) {
    final Instant agora = Instant.now();
    final String comentarioFinal =
        (status == StatusSolicitacao.CONCLUIDA || status == StatusSolicitacao.CANCELADA)
            ? "Comentario final"
            : null;
    return new Solicitacao(
        UUID.randomUUID(),
        "T",
        "D",
        TipoSolicitacao.REPARO,
        status,
        status.exigePrioridade() ? PrioridadeSolicitacao.ALTA : null,
        UUID.randomUUID(),
        UUID.randomUUID(),
        comentarioFinal,
        agora,
        agora,
        status == StatusSolicitacao.CONCLUIDA ? agora : null,
        status == StatusSolicitacao.CANCELADA ? agora : null);
  }

  @Test
  void deveAnexarEvidenciaComSucesso() {
    final Solicitacao sol = criarSolicitacao(StatusSolicitacao.EM_ANDAMENTO);
    final UUID usuarioId = UUID.randomUUID();
    final String url = "http://minio:9000/images/foto.jpg";

    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(storageService.upload(any(), any(), any(), anyLong())).thenReturn(url);
    when(evidenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(solicitacaoEvidenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(atividadeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final var input =
        new AnexarEvidenciaUseCase.Input(
            sol.getId(),
            "foto.jpg",
            "image/jpeg",
            1024L,
            new ByteArrayInputStream(new byte[1024]),
            usuarioId);

    final String publicUrl = useCase.upload(input);
    final Evidencia resultado = useCase.persist(input, publicUrl);

    assertNotNull(resultado);
    assertEquals(url, publicUrl);
    assertEquals("image/jpeg", resultado.getMimeType());
    verify(solicitacaoEvidenciaRepository).save(any());
    verify(atividadeRepository).save(any());
  }

  @Test
  void deveFalharComSolicitacaoNaoEncontrada() {
    when(solicitacaoRepository.findById(any())).thenReturn(Optional.empty());

    assertThrows(
        ValidationException.class,
        () ->
            useCase.upload(
                new AnexarEvidenciaUseCase.Input(
                    UUID.randomUUID(),
                    "foto.jpg",
                    "image/jpeg",
                    1024L,
                    new ByteArrayInputStream(new byte[0]),
                    UUID.randomUUID())));
  }

  @Test
  void deveFalharComSolicitacaoEncerrada() {
    final Solicitacao sol = criarSolicitacao(StatusSolicitacao.CONCLUIDA);

    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));

    assertThrows(
        ValidationException.class,
        () ->
            useCase.upload(
                new AnexarEvidenciaUseCase.Input(
                    sol.getId(),
                    "foto.jpg",
                    "image/jpeg",
                    1024L,
                    new ByteArrayInputStream(new byte[0]),
                    UUID.randomUUID())));
  }

  @Test
  void deveAnexarEvidenciaEmSolicitacaoAFazer() {
    final Solicitacao sol = criarSolicitacao(StatusSolicitacao.A_FAZER);
    final UUID usuarioId = UUID.randomUUID();
    final String url = "http://minio:9000/images/doc.pdf";

    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(storageService.upload(any(), any(), any(), anyLong())).thenReturn(url);
    when(evidenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(solicitacaoEvidenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(atividadeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final var input =
        new AnexarEvidenciaUseCase.Input(
            sol.getId(),
            "doc.pdf",
            "application/pdf",
            2048L,
            new ByteArrayInputStream(new byte[2048]),
            usuarioId);

    final String publicUrl = useCase.upload(input);
    final Evidencia resultado = useCase.persist(input, publicUrl);

    assertNotNull(resultado);
    assertEquals("application/pdf", resultado.getMimeType());
  }
}
