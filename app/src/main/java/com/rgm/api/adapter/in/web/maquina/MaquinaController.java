package com.rgm.api.adapter.in.web.maquina;

import com.rgm.api.adapter.in.web.dto.response.MaquinaResponse;
import com.rgm.api.adapter.in.web.dto.response.PageResponse;
import com.rgm.api.core.application.usecases.admin.ListarMaquinasUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint publico (autenticado) para listar maquinas — acessivel a qualquer perfil. */
@RestController
@RequestMapping("/api/maquinas")
public class MaquinaController {

  private final ListarMaquinasUseCase listarMaquinasUseCase;

  public MaquinaController(final ListarMaquinasUseCase listarMaquinasUseCase) {
    this.listarMaquinasUseCase = listarMaquinasUseCase;
  }

  @GetMapping
  public ResponseEntity<PageResponse<MaquinaResponse>> listar(
      @RequestParam(defaultValue = "0") final int page,
      @RequestParam(defaultValue = "20") final int size) {
    final var result = listarMaquinasUseCase.execute(page, size);
    return ResponseEntity.ok(PageResponse.from(result, MaquinaResponse::from));
  }
}
