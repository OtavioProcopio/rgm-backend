package com.rgm.api.adapter.in.web.maquina;

import com.rgm.api.adapter.in.web.dto.response.MaquinaResponse;
import com.rgm.api.core.application.usecases.maquina.ListarMaquinasUseCase;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/maquinas")
public class MaquinaController {

  private final ListarMaquinasUseCase listarMaquinasUseCase;

  public MaquinaController(final ListarMaquinasUseCase listarMaquinasUseCase) {
    this.listarMaquinasUseCase = listarMaquinasUseCase;
  }

  @GetMapping
  public ResponseEntity<List<MaquinaResponse>> listar() {
    final var maquinas =
        listarMaquinasUseCase.execute().stream().map(MaquinaResponse::from).toList();
    return ResponseEntity.ok(maquinas);
  }
}
