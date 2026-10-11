package com.rgm.api.adapter.in.web.tipomodelo;

import com.rgm.api.adapter.in.web.dto.response.TipoDeModeloResponse;
import com.rgm.api.core.application.usecases.tipomodelo.ListarTiposDeModeloUseCase;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tipos-modelo")
public class TipoDeModeloController {

  private final ListarTiposDeModeloUseCase listarTiposDeModeloUseCase;

  public TipoDeModeloController(final ListarTiposDeModeloUseCase listarTiposDeModeloUseCase) {
    this.listarTiposDeModeloUseCase = listarTiposDeModeloUseCase;
  }

  @GetMapping
  public ResponseEntity<List<TipoDeModeloResponse>> listar() {
    final var tiposDeModelo =
        listarTiposDeModeloUseCase.execute().stream().map(TipoDeModeloResponse::from).toList();
    return ResponseEntity.ok(tiposDeModelo);
  }
}
