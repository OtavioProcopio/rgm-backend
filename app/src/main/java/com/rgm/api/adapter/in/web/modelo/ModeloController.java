package com.rgm.api.adapter.in.web.modelo;

import com.rgm.api.adapter.in.web.dto.request.CriarModeloRequest;
import com.rgm.api.adapter.in.web.dto.request.EditarModeloRequest;
import com.rgm.api.adapter.in.web.dto.request.FotoCapaUploadRequest;
import com.rgm.api.adapter.in.web.dto.response.ModeloResponse;
import com.rgm.api.core.application.usecases.modelo.AtualizarFotoCapaUseCase;
import com.rgm.api.core.application.usecases.modelo.GerenciarModelosUseCase;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/modelos")
public class ModeloController {

  private final GerenciarModelosUseCase gerenciarUseCase;
  private final AtualizarFotoCapaUseCase fotoCapaUseCase;

  public ModeloController(
      final GerenciarModelosUseCase gerenciarUseCase,
      final AtualizarFotoCapaUseCase fotoCapaUseCase) {
    this.gerenciarUseCase = gerenciarUseCase;
    this.fotoCapaUseCase = fotoCapaUseCase;
  }

  @PostMapping
  public ResponseEntity<ModeloResponse> criar(
      @Valid @RequestBody final CriarModeloRequest request, final Authentication authentication) {
    final UUID gestorId = UUID.fromString(authentication.getName());
    final Modelo modelo =
        gerenciarUseCase.criar(
            new GerenciarModelosUseCase.CriarInput(
                request.codigo(),
                request.descricao(),
                request.observacoes(),
                request.maquinaId(),
                gestorId));
    return ResponseEntity.status(HttpStatus.CREATED).body(ModeloResponse.from(modelo));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ModeloResponse> editar(
      @PathVariable final UUID id,
      @Valid @RequestBody final EditarModeloRequest request,
      final Authentication authentication) {
    final UUID gestorId = UUID.fromString(authentication.getName());
    final Modelo modelo =
        gerenciarUseCase.editar(
            new GerenciarModelosUseCase.EditarInput(
                id, request.codigo(), request.descricao(), request.observacoes(), gestorId));
    return ResponseEntity.ok(ModeloResponse.from(modelo));
  }

  @PatchMapping("/{id}/desativar")
  public ResponseEntity<ModeloResponse> desativar(
      @PathVariable final UUID id, final Authentication authentication) {
    final UUID gestorId = UUID.fromString(authentication.getName());
    final Modelo modelo =
        gerenciarUseCase.desativar(new GerenciarModelosUseCase.DesativarInput(id, gestorId));
    return ResponseEntity.ok(ModeloResponse.from(modelo));
  }

  @PostMapping(value = "/{id}/foto-capa", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ModeloResponse> uploadFotoCapa(
      @PathVariable final UUID id,
      @RequestParam("file") final MultipartFile file,
      final Authentication authentication) {
    try {
      final UUID gestorId = UUID.fromString(authentication.getName());
      final Modelo modelo =
          fotoCapaUseCase.executeUpload(
              new AtualizarFotoCapaUseCase.UploadInput(
                  id,
                  file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown",
                  file.getContentType() != null
                      ? file.getContentType()
                      : "application/octet-stream",
                  file.getSize(),
                  file.getInputStream(),
                  gestorId));
      return ResponseEntity.ok(ModeloResponse.from(modelo));
    } catch (final java.io.IOException e) {
      throw new RuntimeException("Erro ao ler arquivo: " + e.getMessage(), e);
    }
  }

  @PatchMapping("/{id}/foto-capa")
  public ResponseEntity<ModeloResponse> usarEvidenciaComoFotoCapa(
      @PathVariable final UUID id,
      @RequestBody final FotoCapaUploadRequest request,
      final Authentication authentication) {
    final UUID gestorId = UUID.fromString(authentication.getName());
    final Modelo modelo =
        fotoCapaUseCase.executeEvidenciaExistente(
            new AtualizarFotoCapaUseCase.EvidenciaExistenteInput(
                id, request.evidenciaId(), gestorId));
    return ResponseEntity.ok(ModeloResponse.from(modelo));
  }
}
