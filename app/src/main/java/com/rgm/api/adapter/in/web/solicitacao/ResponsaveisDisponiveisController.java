package com.rgm.api.adapter.in.web.solicitacao;

import com.rgm.api.adapter.in.web.ParametrosDeConsulta;
import com.rgm.api.adapter.in.web.dto.response.PageResponse;
import com.rgm.api.adapter.in.web.dto.response.ResponsavelDisponivelResponse;
import com.rgm.api.core.application.usecases.solicitacao.ListarResponsaveisDisponiveisUseCase;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Quem pode ser responsavel por uma solicitacao: so para quem tria (gestor e administrador). */
@RestController
@RequestMapping("/api/solicitacoes/responsaveis-disponiveis")
public class ResponsaveisDisponiveisController {

  private final ListarResponsaveisDisponiveisUseCase listarUseCase;

  public ResponsaveisDisponiveisController(
      final ListarResponsaveisDisponiveisUseCase listarUseCase) {
    this.listarUseCase = listarUseCase;
  }

  @GetMapping
  public ResponseEntity<PageResponse<ResponsavelDisponivelResponse>> listar(
      @RequestParam(required = false) final String busca,
      @RequestParam(defaultValue = "0") final int page,
      @RequestParam(defaultValue = "20") final int size,
      final Authentication authentication) {
    ParametrosDeConsulta.pagina(page, size);
    final var resultado =
        listarUseCase.execute(
            new ListarResponsaveisDisponiveisUseCase.Input(
                ParametrosDeConsulta.busca(busca),
                page,
                size,
                UUID.fromString(authentication.getName())));
    return ResponseEntity.ok(PageResponse.from(resultado, ResponsavelDisponivelResponse::from));
  }
}
