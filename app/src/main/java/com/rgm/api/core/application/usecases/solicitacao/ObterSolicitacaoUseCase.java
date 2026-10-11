package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.AcaoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.validation.AcessoSolicitacao;
import com.rgm.api.core.domain.validation.AcoesPermitidasSolicitacao;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ObterSolicitacaoUseCase {

  private final SolicitacaoRepository solicitacaoRepository;
  private final SolicitacaoAtribuicaoRepository atribuicaoRepository;
  private final UsuarioRepository usuarioRepository;
  private final ModeloRepository modeloRepository;

  public ObterSolicitacaoUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final SolicitacaoAtribuicaoRepository atribuicaoRepository,
      final UsuarioRepository usuarioRepository,
      final ModeloRepository modeloRepository) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.atribuicaoRepository = atribuicaoRepository;
    this.usuarioRepository = usuarioRepository;
    this.modeloRepository = modeloRepository;
  }

  public record Input(UUID solicitacaoId, UUID usuarioId) {}

  /** Responsavel com o nome resolvido; o nome e nulo quando o usuario nao e encontrado. */
  public record ResponsavelNome(UUID id, String nome) {}

  /** Identificacao do modelo da solicitacao: codigo, maquina e versao do cadastro. */
  public record ResumoModelo(String codigo, String maquina, int versao) {}

  public record Output(
      Solicitacao solicitacao,
      List<UUID> responsavelIds,
      List<ResponsavelNome> responsaveis,
      String abertaPorNome,
      Set<AcaoSolicitacao> acoesPermitidas,
      ResumoModelo modelo) {

    /** Sem o resumo do modelo. */
    public Output(
        final Solicitacao solicitacao,
        final List<UUID> responsavelIds,
        final List<ResponsavelNome> responsaveis,
        final String abertaPorNome,
        final Set<AcaoSolicitacao> acoesPermitidas) {
      this(solicitacao, responsavelIds, responsaveis, abertaPorNome, acoesPermitidas, null);
    }
  }

  public Output execute(final Input input) {
    final UUID id = input.solicitacaoId();
    final Solicitacao solicitacao =
        solicitacaoRepository
            .findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao nao encontrada"));

    final Usuario usuario =
        usuarioRepository
            .findById(input.usuarioId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));

    final List<UUID> responsavelIds =
        atribuicaoRepository.findBySolicitacaoId(id).stream()
            .filter(a -> a.getRemovidoEm() == null)
            .map(a -> a.getUsuarioId())
            .toList();

    AcessoSolicitacao.validarLeitura(
        usuario, solicitacao, () -> responsavelIds.contains(usuario.getId()));

    final Set<AcaoSolicitacao> acoesPermitidas =
        AcoesPermitidasSolicitacao.calcular(
            usuario,
            solicitacao,
            responsavelIds.contains(usuario.getId()),
            !responsavelIds.isEmpty());

    final List<UUID> idsParaNomear = new ArrayList<>(responsavelIds);
    idsParaNomear.add(solicitacao.getAbertaPorUsuarioId());
    final Map<UUID, String> nomes = resolverNomes(idsParaNomear);
    final List<ResponsavelNome> responsaveis =
        responsavelIds.stream().map(rid -> new ResponsavelNome(rid, nomes.get(rid))).toList();

    return new Output(
        solicitacao,
        responsavelIds,
        responsaveis,
        nomes.get(solicitacao.getAbertaPorUsuarioId()),
        acoesPermitidas,
        resolverModelos(List.of(solicitacao)).get(solicitacao.getModeloId()));
  }

  /**
   * Resolve codigo, maquina e versao dos modelos das solicitacoes numa so consulta; solicitacao sem
   * modelo (ou com modelo removido) nao entra no mapa. O mapa aceita consulta com chave nula
   * (solicitacao de criacao ainda sem modelo).
   */
  public Map<UUID, ResumoModelo> resolverModelos(final Collection<Solicitacao> solicitacoes) {
    final List<UUID> ids =
        solicitacoes.stream()
            .map(Solicitacao::getModeloId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    if (ids.isEmpty()) {
      return new HashMap<>();
    }
    return modeloRepository.findAllByIdIn(ids).stream()
        .collect(
            Collectors.toMap(
                Modelo::getId,
                m -> new ResumoModelo(m.getCodigo(), m.getMaquina(), m.getVersao())));
  }

  /** Resolve o nome de varios usuarios numa so consulta; inativos entram, desconhecidos nao. */
  public Map<UUID, String> resolverNomes(final Collection<UUID> usuarioIds) {
    final List<UUID> ids = usuarioIds.stream().filter(Objects::nonNull).distinct().toList();
    if (ids.isEmpty()) {
      return Map.of();
    }
    return usuarioRepository.findAllByIdIn(ids).stream()
        .collect(Collectors.toMap(Usuario::getId, Usuario::getNome));
  }

  public Map<UUID, List<UUID>> listarResponsaveisBatch(final List<UUID> solicitacaoIds) {
    if (solicitacaoIds.isEmpty()) {
      return Map.of();
    }
    return atribuicaoRepository.findBySolicitacaoIdIn(solicitacaoIds).stream()
        .filter(a -> a.getRemovidoEm() == null)
        .collect(
            Collectors.groupingBy(
                a -> a.getSolicitacaoId(),
                Collectors.mapping(a -> a.getUsuarioId(), Collectors.toList())));
  }
}
