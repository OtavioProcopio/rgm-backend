package com.rgm.api.core.application.usecases.modelo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rgm.api.core.application.usecases.modelo.ObterFichaDoModeloUseCase.Ficha;
import com.rgm.api.core.domain.exceptions.RecursoNaoEncontradoException;
import com.rgm.api.core.domain.model.aggregates.EventoModelo;
import com.rgm.api.core.domain.model.aggregates.FotoGaleriaModelo;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.model.aggregates.Solicitacao;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.entities.AtividadeSolicitacao;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.model.enums.TipoEventoModelo;
import com.rgm.api.core.domain.model.enums.TipoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.AtividadeSolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.EventoModeloRepository;
import com.rgm.api.core.domain.ports.repositories.FotoGaleriaModeloRepository;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.ResumoSolicitacoesModelo;
import com.rgm.api.core.domain.ports.repositories.SolicitacaoRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.StorageService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ObterFichaDoModeloUseCaseTest {

  private ModeloRepository modeloRepository;
  private EventoModeloRepository eventoModeloRepository;
  private SolicitacaoRepository solicitacaoRepository;
  private AtividadeSolicitacaoRepository atividadeRepository;
  private UsuarioRepository usuarioRepository;
  private FotoGaleriaModeloRepository fotoRepository;
  private StorageService storageService;
  private ObterFichaDoModeloUseCase useCase;

  private Modelo modelo;

  @BeforeEach
  void setUp() {
    modeloRepository = mock(ModeloRepository.class);
    eventoModeloRepository = mock(EventoModeloRepository.class);
    solicitacaoRepository = mock(SolicitacaoRepository.class);
    atividadeRepository = mock(AtividadeSolicitacaoRepository.class);
    usuarioRepository = mock(UsuarioRepository.class);
    fotoRepository = mock(FotoGaleriaModeloRepository.class);
    storageService = mock(StorageService.class);
    useCase =
        new ObterFichaDoModeloUseCase(
            modeloRepository,
            eventoModeloRepository,
            solicitacaoRepository,
            atividadeRepository,
            usuarioRepository,
            fotoRepository,
            storageService);
    final Instant agora = Instant.now();
    modelo =
        new Modelo(
            UUID.randomUUID(),
            "MOD-1",
            1,
            "Desc",
            null,
            null,
            null,
            true,
            "FBOX",
            null,
            false,
            agora,
            agora);
    when(modeloRepository.findById(modelo.getId())).thenReturn(Optional.of(modelo));
    when(eventoModeloRepository.findByModeloId(modelo.getId())).thenReturn(List.of());
    when(solicitacaoRepository.findByModeloId(modelo.getId())).thenReturn(List.of());
    when(solicitacaoRepository.resumirPorModelo(modelo.getId()))
        .thenReturn(new ResumoSolicitacoesModelo(0, 0, 0, 0, null, null));
    when(fotoRepository.findByModeloId(modelo.getId())).thenReturn(List.of());
  }

  private Solicitacao solicitacao(final UUID autor) {
    return Solicitacao.abrir(
        "T", "D", TipoSolicitacao.REPARO, modelo.getId(), autor, Instant.now());
  }

  private FotoGaleriaModelo foto(final String id, final String url, final boolean principal) {
    return new FotoGaleriaModelo(
        UUID.randomUUID(), modelo.getId(), url, id, principal, UUID.randomUUID(), Instant.now());
  }

  @Test
  void shouldFailWhenModeloDoesNotExist() {
    // Arrange
    final UUID id = UUID.randomUUID();
    when(modeloRepository.findById(id)).thenReturn(Optional.empty());

    // Act
    final var acao = (org.junit.jupiter.api.function.Executable) () -> useCase.execute(id);

    // Assert
    assertThrows(RecursoNaoEncontradoException.class, acao);
  }

  @Test
  void shouldLoadActivitiesOfAllSolicitacoesInASingleCallWhenBuildingTheFicha() {
    // Arrange
    final UUID autor = UUID.randomUUID();
    final List<Solicitacao> solicitacoes = new ArrayList<>();
    for (int i = 0; i < 50; i++) {
      solicitacoes.add(solicitacao(autor));
    }
    final var atividade =
        AtividadeSolicitacao.comentario(
            solicitacoes.get(0).getId(), "Peça trocada", autor, Instant.now());
    when(solicitacaoRepository.findByModeloId(modelo.getId())).thenReturn(solicitacoes);
    when(atividadeRepository.findBySolicitacaoIdIn(any())).thenReturn(List.of(atividade));
    when(usuarioRepository.findAllByIdIn(any()))
        .thenReturn(
            List.of(
                Usuario.criarInterno(
                    "Maria", "m@x.com", "h", PerfilUsuario.OPERADOR, Instant.now())));

    // Act
    final Ficha ficha = useCase.execute(modelo.getId());

    // Assert
    assertEquals(50, ficha.solicitacoes().size());
    assertEquals(1, ficha.atividadesPorSolicitacao().get(solicitacoes.get(0).getId()).size());
    verify(atividadeRepository, times(1)).findBySolicitacaoIdIn(any());
    verify(atividadeRepository, never()).findBySolicitacaoId(any());
    verify(usuarioRepository, times(1)).findAllByIdIn(any());
  }

  @Test
  void shouldPutTheCoverFirstAndDownloadEachPhoto() {
    // Arrange
    final var comum = foto("Lateral", "http://m/b/lateral.png", false);
    final var capa = foto("Capa", "http://m/b/capa.png", true);
    when(fotoRepository.findByModeloId(modelo.getId())).thenReturn(List.of(comum, capa));
    when(storageService.baixar("http://m/b/capa.png")).thenReturn(new byte[] {1});
    when(storageService.baixar("http://m/b/lateral.png")).thenReturn(new byte[] {2});

    // Act
    final Ficha ficha = useCase.execute(modelo.getId());

    // Assert
    assertEquals("Capa", ficha.fotos().get(0).identificacao());
    assertTrue(ficha.fotos().get(0).principal());
    assertArrayEquals(new byte[] {1}, ficha.fotos().get(0).conteudo());
    assertEquals("Lateral", ficha.fotos().get(1).identificacao());
  }

  @Test
  void shouldKeepThePhotoWithoutContentWhenItCannotBeDownloaded() {
    // Arrange
    final var perdida = foto("Perdida", "http://m/b/x.png", false);
    when(fotoRepository.findByModeloId(modelo.getId())).thenReturn(List.of(perdida));
    when(storageService.baixar("http://m/b/x.png")).thenThrow(new RuntimeException("sumiu"));

    // Act
    final Ficha ficha = useCase.execute(modelo.getId());

    // Assert
    assertEquals(1, ficha.fotos().size());
    assertNull(ficha.fotos().get(0).conteudo());
  }

  @Test
  void shouldCarryTimesAndEventsFromTheModelHistory() {
    // Arrange
    final var evento =
        new EventoModelo(
            UUID.randomUUID(),
            modelo.getId(),
            TipoEventoModelo.MANUTENCAO,
            "Troca",
            "d",
            null,
            UUID.randomUUID(),
            null,
            Instant.now());
    when(eventoModeloRepository.findByModeloId(modelo.getId())).thenReturn(List.of(evento));
    when(solicitacaoRepository.resumirPorModelo(modelo.getId()))
        .thenReturn(new ResumoSolicitacoesModelo(3, 1, 2, 0, 7200.0, 86400.0));

    // Act
    final Ficha ficha = useCase.execute(modelo.getId());

    // Assert
    assertEquals(List.of(evento), ficha.eventos());
    assertEquals(7200.0, ficha.tempoMedioResolucaoSegundos());
    assertEquals(86400.0, ficha.intervaloMedioSegundos());
  }
}
