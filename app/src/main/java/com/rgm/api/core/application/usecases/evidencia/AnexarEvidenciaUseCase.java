package com.rgm.api.core.application.usecases.evidencia;

import com.rgm.api.core.application.usecases.modelo.AdicionarFotoGaleriaUseCase;
import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.Evidencia;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.model.entities.SolicitacaoEvidencia;
import com.rgm.api.core.domain.model.enums.TipoEvidencia;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.EvidenciaRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoEvidenciaRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.StorageService;
import java.io.InputStream;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** UC-08: Anexar evidencia (upload ao MinIO/S3 com publicUrl persistente). */
public final class AnexarEvidenciaUseCase {

  private static final Logger log = LoggerFactory.getLogger(AnexarEvidenciaUseCase.class);

  private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB
  private static final Set<String> ALLOWED_MIME_TYPES =
      Set.of("image/jpeg", "image/png", "image/gif", "image/webp", "application/pdf", "video/mp4");

  private static final Set<TipoEvidencia> TIPOS_ELEGIVEIS_GALERIA =
      Set.of(TipoEvidencia.SERVICO_REALIZADO, TipoEvidencia.CONCLUSAO);
  private static final Set<String> MIME_TYPES_GALERIA =
      Set.of("image/jpeg", "image/png", "image/webp");

  private final SolicitacaoRepository solicitacaoRepository;
  private final EvidenciaRepository evidenciaRepository;
  private final SolicitacaoEvidenciaRepository solicitacaoEvidenciaRepository;
  private final AtividadeSolicitacaoRepository atividadeRepository;
  private final StorageService storageService;
  private final UsuarioRepository usuarioRepository;
  private final SolicitacaoAtribuicaoRepository atribuicaoRepository;
  private final AdicionarFotoGaleriaUseCase adicionarFotoGaleriaUseCase;

  public AnexarEvidenciaUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final EvidenciaRepository evidenciaRepository,
      final SolicitacaoEvidenciaRepository solicitacaoEvidenciaRepository,
      final AtividadeSolicitacaoRepository atividadeRepository,
      final StorageService storageService,
      final UsuarioRepository usuarioRepository,
      final SolicitacaoAtribuicaoRepository atribuicaoRepository,
      final AdicionarFotoGaleriaUseCase adicionarFotoGaleriaUseCase) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.evidenciaRepository = evidenciaRepository;
    this.solicitacaoEvidenciaRepository = solicitacaoEvidenciaRepository;
    this.atividadeRepository = atividadeRepository;
    this.storageService = storageService;
    this.usuarioRepository = usuarioRepository;
    this.atribuicaoRepository = atribuicaoRepository;
    this.adicionarFotoGaleriaUseCase = adicionarFotoGaleriaUseCase;
  }

  public record Input(
      UUID solicitacaoId,
      String nomeArquivo,
      String mimeType,
      long tamanhoBytes,
      InputStream conteudo,
      UUID enviadaPorUsuarioId,
      TipoEvidencia tipo,
      String descricao,
      Supplier<InputStream> conteudoParaGaleria) {

    public Input {
      if (tipo == null) {
        tipo = TipoEvidencia.GERAL;
      }
    }

    /** Conveniencia para chamadores que nunca alimentam a galeria automaticamente (ex.: PDFs). */
    public Input(
        final UUID solicitacaoId,
        final String nomeArquivo,
        final String mimeType,
        final long tamanhoBytes,
        final InputStream conteudo,
        final UUID enviadaPorUsuarioId,
        final TipoEvidencia tipo,
        final String descricao) {
      this(
          solicitacaoId,
          nomeArquivo,
          mimeType,
          tamanhoBytes,
          conteudo,
          enviadaPorUsuarioId,
          tipo,
          descricao,
          null);
    }
  }

  /** Resultado do upload: URL da evidencia e, se elegivel, os dados para persistir na galeria. */
  public record UploadResult(
      String evidenciaPublicUrl,
      AdicionarFotoGaleriaUseCase.Input galeriaInput,
      String galeriaPublicUrl) {}

  public UploadResult upload(final Input input) {

    if (input.tamanhoBytes() > MAX_FILE_SIZE_BYTES) {
      throw new ValidationException(
          "Arquivo excede tamanho maximo permitido de "
              + (MAX_FILE_SIZE_BYTES / (1024 * 1024))
              + " MB");
    }

    if (input.mimeType() != null && !ALLOWED_MIME_TYPES.contains(input.mimeType().toLowerCase())) {
      throw new ValidationException(
          "Tipo de arquivo nao permitido: "
              + input.mimeType()
              + ". Tipos aceitos: "
              + ALLOWED_MIME_TYPES);
    }

    final Solicitacao solicitacao =
        solicitacaoRepository
            .findById(input.solicitacaoId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao nao encontrada"));

    if (solicitacao.getStatus().isTerminal()) {
      throw new BusinessRuleException("Nao e possivel anexar evidencia a solicitacao encerrada");
    }

    validarAcesso(input.solicitacaoId(), input.enviadaPorUsuarioId());

    final String evidenciaPublicUrl =
        storageService.upload(
            input.nomeArquivo(), input.mimeType(), input.conteudo(), input.tamanhoBytes());

    AdicionarFotoGaleriaUseCase.Input galeriaInput = null;
    String galeriaPublicUrl = null;
    if (elegivelParaGaleria(input, solicitacao)) {
      try {
        galeriaInput =
            new AdicionarFotoGaleriaUseCase.Input(
                solicitacao.getModeloId(),
                gerarIdentificacaoGaleria(input.tipo(), solicitacao.getTitulo()),
                input.nomeArquivo(),
                input.mimeType(),
                input.tamanhoBytes(),
                input.conteudoParaGaleria().get(),
                input.enviadaPorUsuarioId());
        galeriaPublicUrl = adicionarFotoGaleriaUseCase.uploadAutomatico(galeriaInput);
      } catch (final RuntimeException e) {
        log.warn(
            "Falha ao copiar evidencia para a galeria do modelo {}: {}",
            solicitacao.getModeloId(),
            e.getMessage());
        galeriaInput = null;
        galeriaPublicUrl = null;
      }
    }

    return new UploadResult(evidenciaPublicUrl, galeriaInput, galeriaPublicUrl);
  }

  public Evidencia persist(final Input input, final UploadResult uploadResult) {
    final Instant agora = Instant.now();

    final Evidencia evidencia =
        Evidencia.criar(
            uploadResult.evidenciaPublicUrl(),
            input.mimeType(),
            input.nomeArquivo(),
            input.tamanhoBytes() > 0 && input.tamanhoBytes() <= Integer.MAX_VALUE
                ? (int) input.tamanhoBytes()
                : null,
            input.enviadaPorUsuarioId(),
            agora,
            input.tipo(),
            input.descricao());

    final Evidencia salva = evidenciaRepository.save(evidencia);

    solicitacaoEvidenciaRepository.save(
        new SolicitacaoEvidencia(input.solicitacaoId(), salva.getId()));

    atividadeRepository.save(
        AtividadeSolicitacao.evidenciaAdicionada(
            input.solicitacaoId(), input.enviadaPorUsuarioId(), agora));

    if (uploadResult.galeriaInput() != null && uploadResult.galeriaPublicUrl() != null) {
      try {
        adicionarFotoGaleriaUseCase.persist(
            uploadResult.galeriaInput(), uploadResult.galeriaPublicUrl());
      } catch (final RuntimeException e) {
        log.warn("Falha ao persistir foto de galeria a partir de evidencia: {}", e.getMessage());
      }
    }

    return salva;
  }

  private static boolean elegivelParaGaleria(final Input input, final Solicitacao solicitacao) {
    return solicitacao.getModeloId() != null
        && TIPOS_ELEGIVEIS_GALERIA.contains(input.tipo())
        && input.mimeType() != null
        && MIME_TYPES_GALERIA.contains(input.mimeType().toLowerCase())
        && input.conteudoParaGaleria() != null;
  }

  private static String gerarIdentificacaoGaleria(final TipoEvidencia tipo, final String titulo) {
    final String rotulo =
        switch (tipo) {
          case SERVICO_REALIZADO -> "Serviço realizado";
          case CONCLUSAO -> "Conclusão";
          default -> tipo.name();
        };
    return rotulo + " — " + titulo;
  }

  private void validarAcesso(final UUID solicitacaoId, final UUID usuarioId) {
    final Usuario usuario =
        usuarioRepository
            .findById(usuarioId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));

    if (usuario.getPerfil().podeGerenciarModelos()
        || usuario.getPerfil().podeGerenciarUsuariosEMaquinas()) {
      return;
    }

    final boolean atribuido =
        atribuicaoRepository.existsBySolicitacaoIdAndUsuarioIdAndRemovidoEmIsNull(
            solicitacaoId, usuarioId);
    if (!atribuido) {
      throw new NaoAutorizadoException("Usuario nao tem acesso a esta solicitacao");
    }
  }
}
