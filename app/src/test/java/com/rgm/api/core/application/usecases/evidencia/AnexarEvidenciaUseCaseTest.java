package com.rgm.api.core.application.usecases.evidencia;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rgm.api.core.application.usecases.modelo.AdicionarFotoGaleriaUseCase;
import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Evidencia;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoEvidencia;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.EvidenciaRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoEvidenciaRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
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
  private UsuarioRepository usuarioRepository;
  private SolicitacaoAtribuicaoRepository atribuicaoRepository;
  private AdicionarFotoGaleriaUseCase adicionarFotoGaleriaUseCase;
  private AnexarEvidenciaUseCase useCase;

  @BeforeEach
  void setUp() {
    solicitacaoRepository = mock(SolicitacaoRepository.class);
    evidenciaRepository = mock(EvidenciaRepository.class);
    solicitacaoEvidenciaRepository = mock(SolicitacaoEvidenciaRepository.class);
    atividadeRepository = mock(AtividadeSolicitacaoRepository.class);
    storageService = mock(StorageService.class);
    usuarioRepository = mock(UsuarioRepository.class);
    atribuicaoRepository = mock(SolicitacaoAtribuicaoRepository.class);
    adicionarFotoGaleriaUseCase = mock(AdicionarFotoGaleriaUseCase.class);
    useCase =
        new AnexarEvidenciaUseCase(
            solicitacaoRepository,
            evidenciaRepository,
            solicitacaoEvidenciaRepository,
            atividadeRepository,
            storageService,
            usuarioRepository,
            atribuicaoRepository,
            adicionarFotoGaleriaUseCase);
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
        null /* modeloCodigo */,
        null /* modeloMaquina */,
        null /* modeloObservacoes */,
        UUID.randomUUID(),
        comentarioFinal,
        agora,
        agora,
        status == StatusSolicitacao.CONCLUIDA ? agora : null,
        status == StatusSolicitacao.CANCELADA ? agora : null);
  }

  private Usuario criarUsuario(final UUID id, final PerfilUsuario perfil) {
    return Usuario.criarInterno("Test", "test@test.com", "hash", perfil, Instant.now());
  }

  @Test
  void deveAnexarEvidenciaComSucesso() {
    final Solicitacao sol = criarSolicitacao(StatusSolicitacao.EM_ANDAMENTO);
    final UUID usuarioId = UUID.randomUUID();
    final String url = "http://minio:9000/images/foto.jpg";

    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(usuarioId))
        .thenReturn(Optional.of(criarUsuario(usuarioId, PerfilUsuario.GESTOR)));
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
            usuarioId,
            null,
            null);

    final AnexarEvidenciaUseCase.UploadResult uploadResult = useCase.upload(input);
    final Evidencia resultado = useCase.persist(input, uploadResult);

    assertNotNull(resultado);
    assertEquals(url, uploadResult.evidenciaPublicUrl());
    assertEquals("image/jpeg", resultado.getMimeType());
    assertEquals(TipoEvidencia.GERAL, resultado.getTipo());
    verify(solicitacaoEvidenciaRepository).save(any());
    verify(atividadeRepository).save(any());
  }

  @Test
  void deveAnexarEvidenciaComTipoEDescricao() {
    final Solicitacao sol = criarSolicitacao(StatusSolicitacao.EM_ANDAMENTO);
    final UUID usuarioId = UUID.randomUUID();
    final String url = "http://minio:9000/images/servico.jpg";

    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(usuarioId))
        .thenReturn(Optional.of(criarUsuario(usuarioId, PerfilUsuario.OPERADOR)));
    when(atribuicaoRepository.existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(
            sol.getId(), usuarioId))
        .thenReturn(true);
    when(storageService.upload(any(), any(), any(), anyLong())).thenReturn(url);
    when(evidenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(solicitacaoEvidenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(atividadeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final var input =
        new AnexarEvidenciaUseCase.Input(
            sol.getId(),
            "servico.jpg",
            "image/jpeg",
            1024L,
            new ByteArrayInputStream(new byte[1024]),
            usuarioId,
            TipoEvidencia.SERVICO_REALIZADO,
            "Servico realizado conforme solicitado");

    final AnexarEvidenciaUseCase.UploadResult uploadResult = useCase.upload(input);
    final Evidencia resultado = useCase.persist(input, uploadResult);

    assertEquals(TipoEvidencia.SERVICO_REALIZADO, resultado.getTipo());
    assertEquals("Servico realizado conforme solicitado", resultado.getDescricao());
    verifyNoInteractions(adicionarFotoGaleriaUseCase);
  }

  @Test
  void deveFalharComServicoRealizadoSemDescricao() {
    final Solicitacao sol = criarSolicitacao(StatusSolicitacao.EM_ANDAMENTO);
    final UUID usuarioId = UUID.randomUUID();
    final String url = "http://minio:9000/images/servico.jpg";

    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(usuarioId))
        .thenReturn(Optional.of(criarUsuario(usuarioId, PerfilUsuario.OPERADOR)));
    when(atribuicaoRepository.existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(
            sol.getId(), usuarioId))
        .thenReturn(true);
    when(storageService.upload(any(), any(), any(), anyLong())).thenReturn(url);

    final var input =
        new AnexarEvidenciaUseCase.Input(
            sol.getId(),
            "servico.jpg",
            "image/jpeg",
            1024L,
            new ByteArrayInputStream(new byte[1024]),
            usuarioId,
            TipoEvidencia.SERVICO_REALIZADO,
            "   ");

    final AnexarEvidenciaUseCase.UploadResult uploadResult = useCase.upload(input);

    assertThrows(ValidationException.class, () -> useCase.persist(input, uploadResult));
  }

  @Test
  void deveFalharComSolicitacaoNaoEncontrada() {
    when(solicitacaoRepository.findById(any())).thenReturn(Optional.empty());

    assertThrows(
        RecursoNaoEncontradoException.class,
        () ->
            useCase.upload(
                new AnexarEvidenciaUseCase.Input(
                    UUID.randomUUID(),
                    "foto.jpg",
                    "image/jpeg",
                    1024L,
                    new ByteArrayInputStream(new byte[0]),
                    UUID.randomUUID(),
                    null,
                    null)));
  }

  @Test
  void deveFalharComSolicitacaoEncerrada() {
    final Solicitacao sol = criarSolicitacao(StatusSolicitacao.CONCLUIDA);

    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));

    assertThrows(
        BusinessRuleException.class,
        () ->
            useCase.upload(
                new AnexarEvidenciaUseCase.Input(
                    sol.getId(),
                    "foto.jpg",
                    "image/jpeg",
                    1024L,
                    new ByteArrayInputStream(new byte[0]),
                    UUID.randomUUID(),
                    null,
                    null)));
  }

  @Test
  void deveAnexarEvidenciaEmSolicitacaoAFazer() {
    final Solicitacao sol = criarSolicitacao(StatusSolicitacao.A_FAZER);
    final UUID usuarioId = UUID.randomUUID();
    final String url = "http://minio:9000/images/doc.pdf";

    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(usuarioId))
        .thenReturn(Optional.of(criarUsuario(usuarioId, PerfilUsuario.ADMINISTRADOR)));
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
            usuarioId,
            null,
            null);

    final AnexarEvidenciaUseCase.UploadResult uploadResult = useCase.upload(input);
    final Evidencia resultado = useCase.persist(input, uploadResult);

    assertNotNull(resultado);
    assertEquals("application/pdf", resultado.getMimeType());
  }

  @Test
  void deveFalharComMimeTypeNaoPermitido() {
    final Solicitacao sol = criarSolicitacao(StatusSolicitacao.EM_ANDAMENTO);
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));

    assertThrows(
        ValidationException.class,
        () ->
            useCase.upload(
                new AnexarEvidenciaUseCase.Input(
                    sol.getId(),
                    "doc.html",
                    "text/html",
                    1024L,
                    new ByteArrayInputStream(new byte[0]),
                    UUID.randomUUID(),
                    null,
                    null)));
  }

  @Test
  void deveAlimentarGaleriaComEvidenciaDeServicoRealizadoEmImagem() {
    final Solicitacao sol = criarSolicitacao(StatusSolicitacao.EM_ANDAMENTO);
    final UUID usuarioId = UUID.randomUUID();
    final String url = "http://minio:9000/images/servico.jpg";
    final String urlGaleria = "http://minio:9000/images/servico-galeria.jpg";

    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(usuarioId))
        .thenReturn(Optional.of(criarUsuario(usuarioId, PerfilUsuario.OPERADOR)));
    when(atribuicaoRepository.existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(
            sol.getId(), usuarioId))
        .thenReturn(true);
    when(storageService.upload(any(), any(), any(), anyLong())).thenReturn(url);
    when(evidenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(solicitacaoEvidenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(atividadeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(adicionarFotoGaleriaUseCase.uploadAutomatico(any())).thenReturn(urlGaleria);

    final var input =
        new AnexarEvidenciaUseCase.Input(
            sol.getId(),
            "servico.jpg",
            "image/jpeg",
            1024L,
            new ByteArrayInputStream(new byte[1024]),
            usuarioId,
            TipoEvidencia.SERVICO_REALIZADO,
            "Servico realizado conforme solicitado",
            () -> new ByteArrayInputStream(new byte[1024]));

    final AnexarEvidenciaUseCase.UploadResult uploadResult = useCase.upload(input);
    useCase.persist(input, uploadResult);

    assertEquals(sol.getModeloId(), uploadResult.galeriaInput().modeloId());
    assertEquals(urlGaleria, uploadResult.galeriaPublicUrl());
    verify(adicionarFotoGaleriaUseCase).uploadAutomatico(any());
    verify(adicionarFotoGaleriaUseCase).persist(uploadResult.galeriaInput(), urlGaleria);
  }

  @Test
  void deveAlimentarGaleriaComEvidenciaDeConclusaoEmImagem() {
    final Solicitacao sol = criarSolicitacao(StatusSolicitacao.EM_VALIDACAO);
    final UUID usuarioId = UUID.randomUUID();
    final String url = "http://minio:9000/images/conclusao.png";

    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(usuarioId))
        .thenReturn(Optional.of(criarUsuario(usuarioId, PerfilUsuario.GESTOR)));
    when(storageService.upload(any(), any(), any(), anyLong())).thenReturn(url);
    when(evidenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(solicitacaoEvidenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(atividadeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(adicionarFotoGaleriaUseCase.uploadAutomatico(any())).thenReturn(url);

    final var input =
        new AnexarEvidenciaUseCase.Input(
            sol.getId(),
            "conclusao.png",
            "image/png",
            1024L,
            new ByteArrayInputStream(new byte[1024]),
            usuarioId,
            TipoEvidencia.CONCLUSAO,
            null,
            () -> new ByteArrayInputStream(new byte[1024]));

    final AnexarEvidenciaUseCase.UploadResult uploadResult = useCase.upload(input);
    useCase.persist(input, uploadResult);

    verify(adicionarFotoGaleriaUseCase).uploadAutomatico(any());
  }

  @Test
  void naoDeveAlimentarGaleriaComTipoNaoElegivel() {
    final Solicitacao sol = criarSolicitacao(StatusSolicitacao.EM_ANDAMENTO);
    final UUID usuarioId = UUID.randomUUID();
    final String url = "http://minio:9000/images/abertura.jpg";

    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(usuarioId))
        .thenReturn(Optional.of(criarUsuario(usuarioId, PerfilUsuario.GESTOR)));
    when(storageService.upload(any(), any(), any(), anyLong())).thenReturn(url);
    when(evidenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(solicitacaoEvidenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(atividadeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final var input =
        new AnexarEvidenciaUseCase.Input(
            sol.getId(),
            "abertura.jpg",
            "image/jpeg",
            1024L,
            new ByteArrayInputStream(new byte[1024]),
            usuarioId,
            TipoEvidencia.ABERTURA,
            null,
            () -> new ByteArrayInputStream(new byte[1024]));

    final AnexarEvidenciaUseCase.UploadResult uploadResult = useCase.upload(input);
    useCase.persist(input, uploadResult);

    assertNull(uploadResult.galeriaPublicUrl());
    verifyNoInteractions(adicionarFotoGaleriaUseCase);
  }

  @Test
  void naoDeveAlimentarGaleriaComFormatoIncompativel() {
    final Solicitacao sol = criarSolicitacao(StatusSolicitacao.EM_ANDAMENTO);
    final UUID usuarioId = UUID.randomUUID();
    final String url = "http://minio:9000/images/servico.gif";

    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(usuarioId))
        .thenReturn(Optional.of(criarUsuario(usuarioId, PerfilUsuario.GESTOR)));
    when(storageService.upload(any(), any(), any(), anyLong())).thenReturn(url);
    when(evidenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(solicitacaoEvidenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(atividadeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final var input =
        new AnexarEvidenciaUseCase.Input(
            sol.getId(),
            "servico.gif",
            "image/gif",
            1024L,
            new ByteArrayInputStream(new byte[1024]),
            usuarioId,
            TipoEvidencia.SERVICO_REALIZADO,
            "Feito",
            () -> new ByteArrayInputStream(new byte[1024]));

    final AnexarEvidenciaUseCase.UploadResult uploadResult = useCase.upload(input);
    useCase.persist(input, uploadResult);

    assertNull(uploadResult.galeriaPublicUrl());
    verifyNoInteractions(adicionarFotoGaleriaUseCase);
  }

  @Test
  void falhaAoCopiarParaGaleriaNaoDerrubaAnexoDaEvidencia() {
    final Solicitacao sol = criarSolicitacao(StatusSolicitacao.EM_ANDAMENTO);
    final UUID usuarioId = UUID.randomUUID();
    final String url = "http://minio:9000/images/servico.jpg";

    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(usuarioId))
        .thenReturn(Optional.of(criarUsuario(usuarioId, PerfilUsuario.GESTOR)));
    when(storageService.upload(any(), any(), any(), anyLong())).thenReturn(url);
    when(evidenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(solicitacaoEvidenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(atividadeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(adicionarFotoGaleriaUseCase.uploadAutomatico(any()))
        .thenThrow(new RuntimeException("storage indisponivel"));

    final var input =
        new AnexarEvidenciaUseCase.Input(
            sol.getId(),
            "servico.jpg",
            "image/jpeg",
            1024L,
            new ByteArrayInputStream(new byte[1024]),
            usuarioId,
            TipoEvidencia.SERVICO_REALIZADO,
            "Feito",
            () -> new ByteArrayInputStream(new byte[1024]));

    final AnexarEvidenciaUseCase.UploadResult uploadResult = useCase.upload(input);
    final Evidencia resultado = useCase.persist(input, uploadResult);

    assertNotNull(resultado);
    assertNull(uploadResult.galeriaPublicUrl());
    verify(adicionarFotoGaleriaUseCase, never()).persist(any(), any());
  }

  private static final String URL_EVIDENCIA = "http://storage/evidencias/foto.jpg";

  private Usuario operador(final UUID id, final boolean ativo) {
    final Instant agora = Instant.now();
    return new Usuario(
        id, "Operador", "operador@rgm.test", "hash", PerfilUsuario.OPERADOR, ativo, agora, agora);
  }

  private Solicitacao solicitacaoAbertaPor(final UUID autorId, final StatusSolicitacao status) {
    final Instant agora = Instant.now();
    return new Solicitacao(
        UUID.randomUUID(),
        "T",
        "D",
        TipoSolicitacao.REPARO,
        status,
        status.exigePrioridade() ? PrioridadeSolicitacao.ALTA : null,
        UUID.randomUUID(),
        null,
        null,
        null,
        autorId,
        status.isTerminal() ? "Comentario final" : null,
        agora,
        agora,
        status == StatusSolicitacao.CONCLUIDA ? agora : null,
        status == StatusSolicitacao.CANCELADA ? agora : null);
  }

  private AnexarEvidenciaUseCase.Input entrada(
      final UUID solicitacaoId,
      final UUID usuarioId,
      final TipoEvidencia tipo,
      final ByteArrayInputStream conteudo) {
    return new AnexarEvidenciaUseCase.Input(
        solicitacaoId,
        "foto.jpg",
        "image/jpeg",
        1024L,
        conteudo,
        usuarioId,
        tipo,
        "Descricao da evidencia");
  }

  private void verificarQueNadaMaisFoiChamado() {
    verifyNoMoreInteractions(
        solicitacaoRepository,
        evidenciaRepository,
        solicitacaoEvidenciaRepository,
        atividadeRepository,
        storageService,
        usuarioRepository,
        atribuicaoRepository,
        adicionarFotoGaleriaUseCase);
  }

  @Test
  void shouldUploadOpeningEvidenceWhenUserOpenedTheSolicitacao() {
    // Arrange
    final UUID autorId = UUID.randomUUID();
    final Solicitacao sol = solicitacaoAbertaPor(autorId, StatusSolicitacao.A_FAZER);
    final ByteArrayInputStream conteudo = new ByteArrayInputStream(new byte[1024]);
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(autorId)).thenReturn(Optional.of(operador(autorId, true)));
    when(atribuicaoRepository.existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(
            sol.getId(), autorId))
        .thenReturn(false);
    when(storageService.upload("foto.jpg", "image/jpeg", conteudo, 1024L))
        .thenReturn(URL_EVIDENCIA);

    // Act
    final AnexarEvidenciaUseCase.UploadResult resultado =
        useCase.upload(entrada(sol.getId(), autorId, TipoEvidencia.ABERTURA, conteudo));

    // Assert
    assertEquals(URL_EVIDENCIA, resultado.evidenciaPublicUrl());
    verify(solicitacaoRepository, times(1)).findById(sol.getId());
    verify(usuarioRepository, times(1)).findById(autorId);
    verify(atribuicaoRepository, times(1))
        .existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(sol.getId(), autorId);
    verify(storageService, times(1)).upload("foto.jpg", "image/jpeg", conteudo, 1024L);
    verificarQueNadaMaisFoiChamado();
  }

  @Test
  void shouldUploadGeneralEvidenceWhenAuthorSendsAfterTriage() {
    // Arrange
    final UUID autorId = UUID.randomUUID();
    final Solicitacao sol = solicitacaoAbertaPor(autorId, StatusSolicitacao.EM_ANDAMENTO);
    final ByteArrayInputStream conteudo = new ByteArrayInputStream(new byte[1024]);
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(autorId)).thenReturn(Optional.of(operador(autorId, true)));
    when(atribuicaoRepository.existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(
            sol.getId(), autorId))
        .thenReturn(false);
    when(storageService.upload("foto.jpg", "image/jpeg", conteudo, 1024L))
        .thenReturn(URL_EVIDENCIA);

    // Act
    final AnexarEvidenciaUseCase.UploadResult resultado =
        useCase.upload(entrada(sol.getId(), autorId, TipoEvidencia.GERAL, conteudo));

    // Assert
    assertEquals(URL_EVIDENCIA, resultado.evidenciaPublicUrl());
    verify(solicitacaoRepository, times(1)).findById(sol.getId());
    verify(usuarioRepository, times(1)).findById(autorId);
    verify(atribuicaoRepository, times(1))
        .existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(sol.getId(), autorId);
    verify(storageService, times(1)).upload("foto.jpg", "image/jpeg", conteudo, 1024L);
    verificarQueNadaMaisFoiChamado();
  }

  @Test
  void shouldDenyUploadWhenAuthorSendsServiceEvidenceWithoutBeingAssigned() {
    // Arrange
    final UUID autorId = UUID.randomUUID();
    final Solicitacao sol = solicitacaoAbertaPor(autorId, StatusSolicitacao.EM_ANDAMENTO);
    final ByteArrayInputStream conteudo = new ByteArrayInputStream(new byte[1024]);
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(autorId)).thenReturn(Optional.of(operador(autorId, true)));
    when(atribuicaoRepository.existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(
            sol.getId(), autorId))
        .thenReturn(false);

    // Act
    final NaoAutorizadoException erro =
        assertThrows(
            NaoAutorizadoException.class,
            () ->
                useCase.upload(
                    entrada(sol.getId(), autorId, TipoEvidencia.SERVICO_REALIZADO, conteudo)));

    // Assert
    assertEquals(
        "Quem abriu a solicitacao so pode anexar evidencia do tipo ABERTURA ou GERAL",
        erro.getMessage());
    verify(solicitacaoRepository, times(1)).findById(sol.getId());
    verify(usuarioRepository, times(1)).findById(autorId);
    verify(atribuicaoRepository, times(1))
        .existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(sol.getId(), autorId);
    verificarQueNadaMaisFoiChamado();
  }

  @Test
  void shouldUploadServiceEvidenceWhenAuthorIsAlsoAssigned() {
    // Arrange
    final UUID autorId = UUID.randomUUID();
    final Solicitacao sol = solicitacaoAbertaPor(autorId, StatusSolicitacao.EM_ANDAMENTO);
    final ByteArrayInputStream conteudo = new ByteArrayInputStream(new byte[1024]);
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(autorId)).thenReturn(Optional.of(operador(autorId, true)));
    when(atribuicaoRepository.existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(
            sol.getId(), autorId))
        .thenReturn(true);
    when(storageService.upload("foto.jpg", "image/jpeg", conteudo, 1024L))
        .thenReturn(URL_EVIDENCIA);

    // Act
    final AnexarEvidenciaUseCase.UploadResult resultado =
        useCase.upload(entrada(sol.getId(), autorId, TipoEvidencia.SERVICO_REALIZADO, conteudo));

    // Assert
    assertEquals(URL_EVIDENCIA, resultado.evidenciaPublicUrl());
    verify(solicitacaoRepository, times(1)).findById(sol.getId());
    verify(usuarioRepository, times(1)).findById(autorId);
    verify(atribuicaoRepository, times(1))
        .existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(sol.getId(), autorId);
    verify(storageService, times(1)).upload("foto.jpg", "image/jpeg", conteudo, 1024L);
    verificarQueNadaMaisFoiChamado();
  }

  @Test
  void shouldDenyUploadWhenOperadorHasNoRelationWithTheSolicitacao() {
    // Arrange
    final UUID estranhoId = UUID.randomUUID();
    final Solicitacao sol = solicitacaoAbertaPor(UUID.randomUUID(), StatusSolicitacao.A_FAZER);
    final ByteArrayInputStream conteudo = new ByteArrayInputStream(new byte[1024]);
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(estranhoId))
        .thenReturn(Optional.of(operador(estranhoId, true)));
    when(atribuicaoRepository.existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(
            sol.getId(), estranhoId))
        .thenReturn(false);

    // Act
    final NaoAutorizadoException erro =
        assertThrows(
            NaoAutorizadoException.class,
            () -> useCase.upload(entrada(sol.getId(), estranhoId, TipoEvidencia.GERAL, conteudo)));

    // Assert
    assertEquals("Usuario nao tem acesso a esta solicitacao", erro.getMessage());
    verify(solicitacaoRepository, times(1)).findById(sol.getId());
    verify(usuarioRepository, times(1)).findById(estranhoId);
    verify(atribuicaoRepository, times(1))
        .existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(sol.getId(), estranhoId);
    verificarQueNadaMaisFoiChamado();
  }

  @Test
  void shouldRejectUploadWhenAuthorSendsToClosedSolicitacao() {
    // Arrange
    final UUID autorId = UUID.randomUUID();
    final Solicitacao sol = solicitacaoAbertaPor(autorId, StatusSolicitacao.CANCELADA);
    final ByteArrayInputStream conteudo = new ByteArrayInputStream(new byte[1024]);
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));

    // Act
    final BusinessRuleException erro =
        assertThrows(
            BusinessRuleException.class,
            () -> useCase.upload(entrada(sol.getId(), autorId, TipoEvidencia.GERAL, conteudo)));

    // Assert
    assertEquals("Nao e possivel anexar evidencia a solicitacao encerrada", erro.getMessage());
    verify(solicitacaoRepository, times(1)).findById(sol.getId());
    verificarQueNadaMaisFoiChamado();
  }

  @Test
  void shouldDenyUploadWhenAuthorIsInactive() {
    // Arrange
    final UUID autorId = UUID.randomUUID();
    final Solicitacao sol = solicitacaoAbertaPor(autorId, StatusSolicitacao.A_FAZER);
    final ByteArrayInputStream conteudo = new ByteArrayInputStream(new byte[1024]);
    when(solicitacaoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(usuarioRepository.findById(autorId)).thenReturn(Optional.of(operador(autorId, false)));

    // Act
    final NaoAutorizadoException erro =
        assertThrows(
            NaoAutorizadoException.class,
            () -> useCase.upload(entrada(sol.getId(), autorId, TipoEvidencia.ABERTURA, conteudo)));

    // Assert
    assertEquals("Usuario inativo", erro.getMessage());
    verify(solicitacaoRepository, times(1)).findById(sol.getId());
    verify(usuarioRepository, times(1)).findById(autorId);
    verificarQueNadaMaisFoiChamado();
  }
}
