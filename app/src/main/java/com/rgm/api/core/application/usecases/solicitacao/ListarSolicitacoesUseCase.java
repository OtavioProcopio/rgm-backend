package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PrioridadeSolicitacao;
import com.rgm.api.core.domain.model.enums.StatusSolicitacao;
import com.rgm.api.core.domain.model.enums.TipoFiltroData;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.validation.AcessoSolicitacao;
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
      Boolean emAberto,
      UUID usuarioAutenticadoId,
      int page,
      int size) {}

  public PageResult<Solicitacao> execute(final Input input) {
    final UUID visivelParaUsuarioId = resolverVisibilidade(input);

    final TipoFiltroData tipoData =
        input.tipoData() != null ? input.tipoData() : TipoFiltroData.CRIACAO;
    final boolean porConclusao = tipoData == TipoFiltroData.CONCLUSAO;
    final boolean emAberto = Boolean.TRUE.equals(input.emAberto());

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
        || emAberto
        || input.responsavelId() != null
        || visivelParaUsuarioId != null) {
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
          input.responsavelId(),
          input.maquina(),
          input.atrasada(),
          emAberto,
          visivelParaUsuarioId,
          input.page(),
          input.size());
    }
    return solicitacaoRepository.findAll(input.page(), input.size());
  }

  /** Operador so lista o que abriu ou de que e responsavel; os demais perfis listam tudo. */
  private UUID resolverVisibilidade(final Input input) {
    if (input.usuarioAutenticadoId() == null) {
      return null;
    }
    final Usuario usuario =
        usuarioRepository
            .findById(input.usuarioAutenticadoId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));
    return AcessoSolicitacao.veTodas(usuario) ? null : input.usuarioAutenticadoId();
  }
}
