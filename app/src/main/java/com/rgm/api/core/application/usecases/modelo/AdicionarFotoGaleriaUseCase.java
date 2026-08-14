package com.rgm.api.core.application.usecases.modelo;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.FotoGaleriaModelo;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.ports.repositories.FotoGaleriaModeloRepository;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.StorageService;
import java.io.InputStream;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Adiciona uma foto na galeria (apresentacao) do Modelo. Ator: Gestor/Administrador. */
public final class AdicionarFotoGaleriaUseCase {

  private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB
  private static final Set<String> ALLOWED_MIME_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp");

  private final ModeloRepository modeloRepository;
  private final UsuarioRepository usuarioRepository;
  private final FotoGaleriaModeloRepository fotoGaleriaModeloRepository;
  private final StorageService storageService;

  public AdicionarFotoGaleriaUseCase(
      final ModeloRepository modeloRepository,
      final UsuarioRepository usuarioRepository,
      final FotoGaleriaModeloRepository fotoGaleriaModeloRepository,
      final StorageService storageService) {
    this.modeloRepository = modeloRepository;
    this.usuarioRepository = usuarioRepository;
    this.fotoGaleriaModeloRepository = fotoGaleriaModeloRepository;
    this.storageService = storageService;
  }

  public record Input(
      UUID modeloId,
      String identificacao,
      String nomeArquivo,
      String mimeType,
      long tamanhoBytes,
      InputStream conteudo,
      UUID gestorId) {}

  public String upload(final Input input) {
    validarPermissao(input.gestorId());

    if (input.identificacao() == null || input.identificacao().isBlank()) {
      throw new ValidationException("Identificacao da foto e obrigatoria");
    }

    if (input.tamanhoBytes() > MAX_FILE_SIZE_BYTES) {
      throw new ValidationException(
          "Arquivo excede tamanho maximo permitido de "
              + (MAX_FILE_SIZE_BYTES / (1024 * 1024))
              + " MB");
    }

    if (input.mimeType() == null || !ALLOWED_MIME_TYPES.contains(input.mimeType().toLowerCase())) {
      throw new ValidationException(
          "Tipo de arquivo nao permitido: "
              + input.mimeType()
              + ". Tipos aceitos: "
              + ALLOWED_MIME_TYPES);
    }

    modeloRepository
        .findById(input.modeloId())
        .orElseThrow(() -> new RecursoNaoEncontradoException("Modelo nao encontrado"));

    return storageService.upload(
        input.nomeArquivo(), input.mimeType(), input.conteudo(), input.tamanhoBytes());
  }

  public FotoGaleriaModelo persist(final Input input, final String publicUrl) {
    final Instant agora = Instant.now();

    final boolean primeiraFoto =
        fotoGaleriaModeloRepository.findByModeloId(input.modeloId()).isEmpty();

    final FotoGaleriaModelo foto =
        FotoGaleriaModelo.criar(
            input.modeloId(),
            publicUrl,
            input.identificacao().trim(),
            primeiraFoto,
            input.gestorId(),
            agora);

    return fotoGaleriaModeloRepository.save(foto);
  }

  private void validarPermissao(final UUID gestorId) {
    final Usuario gestor =
        usuarioRepository
            .findById(gestorId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Gestor nao encontrado"));

    if (!gestor.getPerfil().podeGerenciarGaleriaModelo()) {
      throw new NaoAutorizadoException("Perfil sem permissao para gerenciar galeria do Modelo");
    }
  }
}
