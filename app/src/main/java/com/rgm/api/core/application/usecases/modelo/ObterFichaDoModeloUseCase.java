package com.rgm.api.core.application.usecases.modelo;

import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.EventoModelo;
import com.rgm.api.core.domain.model.aggregates.FotoGaleriaModelo;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.EventoModeloRepository;
import com.rgm.api.core.domain.ports.repositories.FotoGaleriaModeloRepository;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.ResumoSolicitacoesModelo;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.StorageService;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reune tudo que a ficha de detalhe do modelo mostra: dados, eventos, solicitacoes com suas
 * atividades, tempos e fotos da galeria (capa primeiro). Numero fixo de consultas: as atividades e
 * os nomes vem em lote e cada foto e uma leitura no armazenamento. Foto que nao pode ser lida vem
 * sem conteudo, e a ficha segue.
 */
public final class ObterFichaDoModeloUseCase {

  private static final Logger log = LoggerFactory.getLogger(ObterFichaDoModeloUseCase.class);

  private final ModeloRepository modeloRepository;
  private final EventoModeloRepository eventoModeloRepository;
  private final SolicitacaoRepository solicitacaoRepository;
  private final AtividadeSolicitacaoRepository atividadeRepository;
  private final UsuarioRepository usuarioRepository;
  private final FotoGaleriaModeloRepository fotoGaleriaModeloRepository;
  private final StorageService storageService;

  public ObterFichaDoModeloUseCase(
      final ModeloRepository modeloRepository,
      final EventoModeloRepository eventoModeloRepository,
      final SolicitacaoRepository solicitacaoRepository,
      final AtividadeSolicitacaoRepository atividadeRepository,
      final UsuarioRepository usuarioRepository,
      final FotoGaleriaModeloRepository fotoGaleriaModeloRepository,
      final StorageService storageService) {
    this.modeloRepository = modeloRepository;
    this.eventoModeloRepository = eventoModeloRepository;
    this.solicitacaoRepository = solicitacaoRepository;
    this.atividadeRepository = atividadeRepository;
    this.usuarioRepository = usuarioRepository;
    this.fotoGaleriaModeloRepository = fotoGaleriaModeloRepository;
    this.storageService = storageService;
  }

  /** Foto da galeria; {@code conteudo} e nulo quando nao pode ser lida. */
  public record FotoDaFicha(String identificacao, boolean principal, byte[] conteudo) {}

  public record Ficha(
      Modelo modelo,
      List<EventoModelo> eventos,
      List<Solicitacao> solicitacoes,
      Map<UUID, List<AtividadeSolicitacao>> atividadesPorSolicitacao,
      Map<UUID, String> nomesPorUsuario,
      Double tempoMedioResolucaoSegundos,
      Double intervaloMedioSegundos,
      List<FotoDaFicha> fotos) {}

  public Ficha execute(final UUID modeloId) {
    final Modelo modelo =
        modeloRepository
            .findById(modeloId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Modelo nao encontrado"));

    final List<EventoModelo> eventos = eventoModeloRepository.findByModeloId(modeloId);
    final List<Solicitacao> solicitacoes = solicitacaoRepository.findByModeloId(modeloId);
    final ResumoSolicitacoesModelo resumo = solicitacaoRepository.resumirPorModelo(modeloId);

    final List<UUID> solicitacaoIds = solicitacoes.stream().map(Solicitacao::getId).toList();
    final Map<UUID, List<AtividadeSolicitacao>> atividades =
        atividadeRepository.findBySolicitacaoIdIn(solicitacaoIds).stream()
            .collect(Collectors.groupingBy(AtividadeSolicitacao::getSolicitacaoId));

    return new Ficha(
        modelo,
        eventos,
        solicitacoes,
        atividades,
        nomes(solicitacoes, atividades, eventos),
        resumo.tempoMedioResolucaoSegundos(),
        resumo.intervaloMedioSegundos(),
        fotos(modeloId));
  }

  private Map<UUID, String> nomes(
      final List<Solicitacao> solicitacoes,
      final Map<UUID, List<AtividadeSolicitacao>> atividades,
      final List<EventoModelo> eventos) {
    final Set<UUID> ids = new HashSet<>();
    eventos.stream().map(EventoModelo::getExecutadoPorUsuarioId).forEach(ids::add);
    solicitacoes.stream().map(Solicitacao::getAbertaPorUsuarioId).forEach(ids::add);
    atividades.values().stream()
        .flatMap(List::stream)
        .map(AtividadeSolicitacao::getAutorUsuarioId)
        .forEach(ids::add);
    ids.removeIf(Objects::isNull);
    if (ids.isEmpty()) {
      return Map.of();
    }
    return usuarioRepository.findAllByIdIn(List.copyOf(ids)).stream()
        .collect(Collectors.toMap(Usuario::getId, Usuario::getNome, (a, b) -> a));
  }

  private List<FotoDaFicha> fotos(final UUID modeloId) {
    return fotoGaleriaModeloRepository.findByModeloId(modeloId).stream()
        .sorted(Comparator.comparing((FotoGaleriaModelo f) -> !f.isPrincipal()))
        .map(f -> new FotoDaFicha(f.getIdentificacao(), f.isPrincipal(), lerFoto(f.getPublicUrl())))
        .collect(Collectors.toList());
  }

  private byte[] lerFoto(final String url) {
    try {
      return storageService.baixar(url);
    } catch (final RuntimeException e) {
      log.warn("Foto da galeria indisponivel para a ficha: {}", e.getMessage());
      return null;
    }
  }
}
