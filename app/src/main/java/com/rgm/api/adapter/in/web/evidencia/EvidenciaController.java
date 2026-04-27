package com.rgm.api.adapter.in.web.evidencia;

import com.rgm.api.adapter.in.web.dto.response.EvidenciaResponse;
import com.rgm.api.core.application.usecases.evidencia.AnexarEvidenciaUseCase;
import com.rgm.api.core.application.usecases.evidencia.VisualizarEvidenciaUseCase;
import com.rgm.api.core.domain.model.aggregates.Evidencia;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/solicitacoes/{solicitacaoId}/evidencias")
public class EvidenciaController {
  private static final Logger log = LoggerFactory.getLogger(EvidenciaController.class);

  private final AnexarEvidenciaUseCase anexarUseCase;
  private final VisualizarEvidenciaUseCase visualizarUseCase;

  public EvidenciaController(
      final AnexarEvidenciaUseCase anexarUseCase,
      final VisualizarEvidenciaUseCase visualizarUseCase) {
    this.anexarUseCase = anexarUseCase;
    this.visualizarUseCase = visualizarUseCase;
  }

  @Transactional
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<EvidenciaResponse> anexar(
      @PathVariable final UUID solicitacaoId,
      @RequestParam("file") final MultipartFile file,
      final Authentication authentication) {
    try {
      final UUID usuarioId = UUID.fromString(authentication.getName());
      final Evidencia evidencia =
          anexarUseCase.execute(
              new AnexarEvidenciaUseCase.Input(
                  solicitacaoId,
                  file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown",
                  file.getContentType() != null
                      ? file.getContentType()
                      : "application/octet-stream",
                  file.getSize(),
                  file.getInputStream(),
                  usuarioId));
      return ResponseEntity.status(HttpStatus.CREATED).body(EvidenciaResponse.from(evidencia));
    } catch (final java.io.IOException e) {
      throw new RuntimeException("Erro ao ler arquivo: " + e.getMessage(), e);
    }
  }

  @GetMapping
  public ResponseEntity<List<EvidenciaResponse>> listar(@PathVariable final UUID solicitacaoId) {
    log.info("EvidenciaController.listar iniciado");
    final List<Evidencia> evidencias =
        visualizarUseCase.execute(new VisualizarEvidenciaUseCase.Input(solicitacaoId));
    return ResponseEntity.ok(evidencias.stream().map(EvidenciaResponse::from).toList());
  }
}
