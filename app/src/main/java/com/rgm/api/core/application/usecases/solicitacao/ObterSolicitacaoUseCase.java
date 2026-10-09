package com.rgm.api.core.application.usecases.solicitacao;

import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.AcaoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoAtribuicaoRepository;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.validation.AcessoSolicitacao;
import com.rgm.api.core.domain.validation.AcoesPermitidasSolicitacao;
import java.util.ArrayList;
import java.util.Collection;
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

  public ObterSolicitacaoUseCase(
      final SolicitacaoRepository solicitacaoRepository,
      final SolicitacaoAtribuicaoRepository atribuicaoRepository,
      final UsuarioRepository usuarioRepository) {
    this.solicitacaoRepository = solicitacaoRepository;
    this.atribuicaoRepository = atribuicaoRepository;
    this.usuarioRepository = usuarioRepository;
  }

  public record Input(UUID solicitacaoId, UUID usuarioId) {}

  /** Responsavel com o nome resolvido; o nome e nulo quando o usuario nao e encontrado. */
  public record ResponsavelNome(UUID id, String nome) {}

  public record Output(
      Solicitacao solicitacao,
      List<UUID> responsavelIds,
      List<ResponsavelNome> responsaveis,
      String abertaPorNome,
      Set<AcaoSolicitacao> acoesPermitidas) {}

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
        acoesPermitidas);
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
