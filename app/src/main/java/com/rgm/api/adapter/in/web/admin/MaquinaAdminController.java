package com.rgm.api.adapter.in.web.admin;

import com.rgm.api.adapter.in.web.dto.request.CriarMaquinaRequest;
import com.rgm.api.adapter.in.web.dto.request.EditarMaquinaRequest;
import com.rgm.api.adapter.in.web.dto.response.MaquinaResponse;
import com.rgm.api.core.application.usecases.admin.GerenciarMaquinasUseCase;
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
@RequestMapping("/api/admin/maquinas")
public class MaquinaAdminController {
  private static final Logger log = LoggerFactory.getLogger(MaquinaAdminController.class);

  private final GerenciarMaquinasUseCase gerenciarMaquinasUseCase;

  public MaquinaAdminController(final GerenciarMaquinasUseCase gerenciarMaquinasUseCase) {
    this.gerenciarMaquinasUseCase = gerenciarMaquinasUseCase;
  }

  @PostMapping
  public ResponseEntity<MaquinaResponse> criar(
      @Valid @RequestBody final CriarMaquinaRequest request, final Authentication authentication) {
    log.info("MaquinaAdminController.criar iniciado");
    final UUID adminId = UUID.fromString(authentication.getName());
    final var maquina =
        gerenciarMaquinasUseCase.criar(
            new GerenciarMaquinasUseCase.CriarInput(request.nome(), adminId));
    return ResponseEntity.status(HttpStatus.CREATED).body(MaquinaResponse.from(maquina));
  }

  @PutMapping("/{id}")
  public ResponseEntity<MaquinaResponse> renomear(
      @PathVariable final UUID id,
      @Valid @RequestBody final EditarMaquinaRequest request,
      final Authentication authentication) {
    log.info("MaquinaAdminController.renomear id={}", id);
    final UUID adminId = UUID.fromString(authentication.getName());
    final var maquina =
        gerenciarMaquinasUseCase.renomear(
            new GerenciarMaquinasUseCase.RenomearInput(id, request.nome(), adminId));
    return ResponseEntity.ok(MaquinaResponse.from(maquina));
  }

  @PatchMapping("/{id}/desativar")
  public ResponseEntity<MaquinaResponse> desativar(
      @PathVariable final UUID id, final Authentication authentication) {
    log.info("MaquinaAdminController.desativar id={}", id);
    final UUID adminId = UUID.fromString(authentication.getName());
    final var maquina =
        gerenciarMaquinasUseCase.desativar(
            new GerenciarMaquinasUseCase.DesativarInput(id, adminId));
    return ResponseEntity.ok(MaquinaResponse.from(maquina));
  }

  @PatchMapping("/{id}/ativar")
  public ResponseEntity<MaquinaResponse> ativar(
      @PathVariable final UUID id, final Authentication authentication) {
    log.info("MaquinaAdminController.ativar id={}", id);
    final UUID adminId = UUID.fromString(authentication.getName());
    final var maquina =
        gerenciarMaquinasUseCase.ativar(new GerenciarMaquinasUseCase.AtivarInput(id, adminId));
    return ResponseEntity.ok(MaquinaResponse.from(maquina));
  }
}
