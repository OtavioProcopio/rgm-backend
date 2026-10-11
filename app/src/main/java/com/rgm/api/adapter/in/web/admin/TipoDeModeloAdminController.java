package com.rgm.api.adapter.in.web.admin;

import com.rgm.api.adapter.in.web.dto.request.CriarTipoDeModeloRequest;
import com.rgm.api.adapter.in.web.dto.request.EditarTipoDeModeloRequest;
import com.rgm.api.adapter.in.web.dto.response.TipoDeModeloResponse;
import com.rgm.api.core.application.usecases.admin.GerenciarTiposDeModeloUseCase;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tipos-modelo")
public class TipoDeModeloAdminController {
  private static final Logger log = LoggerFactory.getLogger(TipoDeModeloAdminController.class);

  private final GerenciarTiposDeModeloUseCase gerenciarTiposDeModeloUseCase;

  public TipoDeModeloAdminController(
      final GerenciarTiposDeModeloUseCase gerenciarTiposDeModeloUseCase) {
    this.gerenciarTiposDeModeloUseCase = gerenciarTiposDeModeloUseCase;
  }

  @PostMapping
  public ResponseEntity<TipoDeModeloResponse> criar(
      @Valid @RequestBody final CriarTipoDeModeloRequest request,
      final Authentication authentication) {
    log.info("TipoDeModeloAdminController.criar iniciado");
    final UUID adminId = UUID.fromString(authentication.getName());
    final var tipoDeModelo =
        gerenciarTiposDeModeloUseCase.criar(
            new GerenciarTiposDeModeloUseCase.CriarInput(request.nome(), adminId));
    return ResponseEntity.status(HttpStatus.CREATED).body(TipoDeModeloResponse.from(tipoDeModelo));
  }

  @PutMapping("/{id}")
  public ResponseEntity<TipoDeModeloResponse> renomear(
      @PathVariable final UUID id,
      @Valid @RequestBody final EditarTipoDeModeloRequest request,
      final Authentication authentication) {
    log.info("TipoDeModeloAdminController.renomear id={}", id);
    final UUID adminId = UUID.fromString(authentication.getName());
    final var tipoDeModelo =
        gerenciarTiposDeModeloUseCase.renomear(
            new GerenciarTiposDeModeloUseCase.RenomearInput(id, request.nome(), adminId));
    return ResponseEntity.ok(TipoDeModeloResponse.from(tipoDeModelo));
  }

  @PatchMapping("/{id}/desativar")
  public ResponseEntity<TipoDeModeloResponse> desativar(
      @PathVariable final UUID id, final Authentication authentication) {
    log.info("TipoDeModeloAdminController.desativar id={}", id);
    final UUID adminId = UUID.fromString(authentication.getName());
    final var tipoDeModelo =
        gerenciarTiposDeModeloUseCase.desativar(
            new GerenciarTiposDeModeloUseCase.DesativarInput(id, adminId));
    return ResponseEntity.ok(TipoDeModeloResponse.from(tipoDeModelo));
  }

  @PatchMapping("/{id}/ativar")
  public ResponseEntity<TipoDeModeloResponse> ativar(
      @PathVariable final UUID id, final Authentication authentication) {
    log.info("TipoDeModeloAdminController.ativar id={}", id);
    final UUID adminId = UUID.fromString(authentication.getName());
    final var tipoDeModelo =
        gerenciarTiposDeModeloUseCase.ativar(
            new GerenciarTiposDeModeloUseCase.AtivarInput(id, adminId));
    return ResponseEntity.ok(TipoDeModeloResponse.from(tipoDeModelo));
  }
}
