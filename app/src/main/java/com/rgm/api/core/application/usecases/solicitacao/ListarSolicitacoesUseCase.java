package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoFiltroData;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.UUID;

/** Listar solicitacoes com paginacao e filtros opcionais. */
public final class ListarSolicitacoesUseCase {

  private final SolicitacaoRepository solicitacaoRepository;
  private final UsuarioRepository usuarioRepository;

  public ListarSolicitacoesUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final UsuarioRepository usuarioRepository) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.usuarioRepository = usuarioRepository;
  }

  public record Input(
      StatusSolicitacao status,
      UUID modeloId,
      TipoSolicitacao tipo,
      PrioridadeSolicitacao prioridade,
      TipoFiltroData tipoData,
      Instant dataInicio,
      Instant dataFim,
      UUID abertaPorUsuarioId,
      UUID responsavelId,
      String maquina,
      Boolean atrasada,
      UUID usuarioAutenticadoId,
      int page,
      int size) {}

  public PageResult<Solicitacao> execute(final Input input) {
    final UUID responsavelId = resolverResponsavel(input);

    final TipoFiltroData tipoData =
        input.tipoData() != null ? input.tipoData() : TipoFiltroData.CRIACAO;
    final boolean porConclusao = tipoData == TipoFiltroData.CONCLUSAO;

    final Instant criadaEmInicio = porConclusao ? null : input.dataInicio();
    final Instant criadaEmFim = porConclusao ? null : input.dataFim();
    final Instant concluidaEmInicio = porConclusao ? input.dataInicio() : null;
    final Instant concluidaEmFim = porConclusao ? input.dataFim() : null;

    if (input.status() != null
        || input.modeloId() != null
        || input.tipo() != null
        || input.prioridade() != null
        || input.dataInicio() != null
        || input.dataFim() != null
        || input.abertaPorUsuarioId() != null
        || input.maquina() != null
        || input.atrasada() != null
        || responsavelId != null) {
      return solicitacaoRepository.findByFilters(
          input.status(),
          input.modeloId(),
          input.tipo(),
          input.prioridade(),
          criadaEmInicio,
          criadaEmFim,
          concluidaEmInicio,
          concluidaEmFim,
          input.abertaPorUsuarioId(),
          responsavelId,
          input.maquina(),
          input.atrasada(),
          input.page(),
          input.size());
    }
    return solicitacaoRepository.findAll(input.page(), input.size());
  }

  private UUID resolverResponsavel(final Input input) {
    if (input.usuarioAutenticadoId() == null) {
      return input.responsavelId();
    }
    final Usuario usuario =
        usuarioRepository
            .findById(input.usuarioAutenticadoId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));
    if (usuario.getPerfil() == PerfilUsuario.OPERADOR) {
      return input.usuarioAutenticadoId();
    }
    return input.responsavelId();
  }
}
