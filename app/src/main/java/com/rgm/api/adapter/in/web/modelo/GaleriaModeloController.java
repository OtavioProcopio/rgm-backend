package com.rgm.api.adapter.in.web.modelo;

import com.rgm.api.adapter.in.web.dto.request.EditarFotoGaleriaRequest;
import com.rgm.api.adapter.in.web.dto.response.FotoGaleriaResponse;
import com.rgm.api.core.application.usecases.modelo.AdicionarFotoGaleriaUseCase;
import com.rgm.api.core.application.usecases.modelo.EditarFotoGaleriaUseCase;
import com.rgm.api.core.application.usecases.modelo.ListarGaleriaModeloUseCase;
import com.rgm.api.core.application.usecases.modelo.RemoverFotoGaleriaUseCase;
import com.rgm.api.core.domain.model.aggregates.FotoGaleriaModelo;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/modelos/{modeloId}/galeria")
public class GaleriaModeloController {
  private static final Logger log = LoggerFactory.getLogger(GaleriaModeloController.class);

  private final AdicionarFotoGaleriaUseCase adicionarUseCase;
  private final ListarGaleriaModeloUseCase listarUseCase;
  private final EditarFotoGaleriaUseCase editarUseCase;
  private final RemoverFotoGaleriaUseCase removerUseCase;

  public GaleriaModeloController(
      final AdicionarFotoGaleriaUseCase adicionarUseCase,
      final ListarGaleriaModeloUseCase listarUseCase,
      final EditarFotoGaleriaUseCase editarUseCase,
      final RemoverFotoGaleriaUseCase removerUseCase) {
    this.adicionarUseCase = adicionarUseCase;
    this.listarUseCase = listarUseCase;
    this.editarUseCase = editarUseCase;
    this.removerUseCase = removerUseCase;
  }

  @GetMapping
  public ResponseEntity<List<FotoGaleriaResponse>> listar(@PathVariable final UUID modeloId) {
    log.info("GaleriaModeloController.listar modeloId={}", modeloId);
    final List<FotoGaleriaModelo> fotos = listarUseCase.execute(modeloId);
    return ResponseEntity.ok(fotos.stream().map(FotoGaleriaResponse::from).toList());
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<FotoGaleriaResponse> adicionar(
      @PathVariable final UUID modeloId,
      @RequestParam("file") final MultipartFile file,
      @RequestParam("identificacao") final String identificacao,
      final Authentication authentication) {
    try {
      final UUID gestorId = UUID.fromString(authentication.getName());
      final var input =
          new AdicionarFotoGaleriaUseCase.Input(
              modeloId,
              identificacao,
              file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown",
              file.getContentType() != null ? file.getContentType() : "application/octet-stream",
              file.getSize(),
              file.getInputStream(),
              gestorId);

      final String publicUrl = adicionarUseCase.upload(input);
      final FotoGaleriaModelo foto = adicionarUseCase.persist(input, publicUrl);
      return ResponseEntity.status(HttpStatus.CREATED).body(FotoGaleriaResponse.from(foto));
    } catch (final java.io.IOException e) {
      throw new RuntimeException("Erro ao ler arquivo: " + e.getMessage(), e);
    }
  }

  @PatchMapping("/{fotoId}")
  public ResponseEntity<FotoGaleriaResponse> editar(
      @PathVariable final UUID modeloId,
      @PathVariable final UUID fotoId,
      @Valid @RequestBody final EditarFotoGaleriaRequest request,
      final Authentication authentication) {
    log.info("GaleriaModeloController.editar modeloId={} fotoId={}", modeloId, fotoId);
    final UUID gestorId = UUID.fromString(authentication.getName());
    final FotoGaleriaModelo foto =
        editarUseCase.execute(
            new EditarFotoGaleriaUseCase.Input(
                modeloId, fotoId, request.identificacao(), request.principal(), gestorId));
    return ResponseEntity.ok(FotoGaleriaResponse.from(foto));
  }

  @DeleteMapping("/{fotoId}")
  public ResponseEntity<Void> remover(
      @PathVariable final UUID modeloId,
      @PathVariable final UUID fotoId,
      final Authentication authentication) {
    log.info("GaleriaModeloController.remover modeloId={} fotoId={}", modeloId, fotoId);
    final UUID gestorId = UUID.fromString(authentication.getName());
    removerUseCase.execute(new RemoverFotoGaleriaUseCase.Input(modeloId, fotoId, gestorId));
    return ResponseEntity.noContent().build();
  }
}
