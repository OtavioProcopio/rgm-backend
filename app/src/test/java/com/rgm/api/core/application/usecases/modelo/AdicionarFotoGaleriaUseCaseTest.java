package com.rgm.api.core.application.usecases.modelo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.FotoGaleriaModelo;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.FotoGaleriaModeloRepository;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.StorageService;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdicionarFotoGaleriaUseCaseTest {

  private ModeloRepository modeloRepository;
  private UsuarioRepository usuarioRepository;
  private FotoGaleriaModeloRepository fotoGaleriaModeloRepository;
  private StorageService storageService;
  private AdicionarFotoGaleriaUseCase useCase;

  @BeforeEach
  void setUp() {
    modeloRepository = mock(ModeloRepository.class);
    usuarioRepository = mock(UsuarioRepository.class);
    fotoGaleriaModeloRepository = mock(FotoGaleriaModeloRepository.class);
    storageService = mock(StorageService.class);
    useCase =
        new AdicionarFotoGaleriaUseCase(
            modeloRepository, usuarioRepository, fotoGaleriaModeloRepository, storageService);
  }

  private Usuario criarGestor() {
    final Instant agora = Instant.now();
    return new Usuario(
        UUID.randomUUID(),
        "Gestor",
        "g@test.com",
        "hash",
        PerfilUsuario.GESTOR,
        true,
        agora,
        agora);
  }

  private Modelo criarModelo() {
    return Modelo.criar("COD-01", "Desc", null, "FBOX", 1, Instant.now());
  }

  @Test
  void deveAdicionarFotoNaGaleria() {
    final Usuario gestor = criarGestor();
    final Modelo modelo = criarModelo();
    final String url = "http://minio:9000/images/nova.jpg";

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(modeloRepository.findById(modelo.getId())).thenReturn(Optional.of(modelo));
    when(storageService.upload(any(), any(), any(), anyLong())).thenReturn(url);
    when(fotoGaleriaModeloRepository.findByModeloId(modelo.getId())).thenReturn(List.of());
    when(fotoGaleriaModeloRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final var input =
        new AdicionarFotoGaleriaUseCase.Input(
            modelo.getId(),
            "Parte 1",
            "nova.jpg",
            "image/jpeg",
            512L,
            new ByteArrayInputStream(new byte[512]),
            gestor.getId());

    final String publicUrl = useCase.upload(input);
    final FotoGaleriaModelo resultado = useCase.persist(input, publicUrl);

    assertEquals(url, resultado.getPublicUrl());
    assertEquals("Parte 1", resultado.getIdentificacao());
    assertTrue(resultado.isPrincipal(), "primeira foto deve virar principal automaticamente");
  }

  @Test
  void segundaFotoNaoViraPrincipalAutomaticamente() {
    final Usuario gestor = criarGestor();
    final Modelo modelo = criarModelo();

    when(fotoGaleriaModeloRepository.findByModeloId(modelo.getId()))
        .thenReturn(
            List.of(
                FotoGaleriaModelo.criar(
                    modelo.getId(),
                    "http://x/1.jpg",
                    "Parte 1",
                    true,
                    gestor.getId(),
                    Instant.now())));
    when(fotoGaleriaModeloRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final var input =
        new AdicionarFotoGaleriaUseCase.Input(
            modelo.getId(),
            "Parte 2",
            "nova.jpg",
            "image/jpeg",
            512L,
            new ByteArrayInputStream(new byte[512]),
            gestor.getId());

    final FotoGaleriaModelo resultado = useCase.persist(input, "http://x/2.jpg");

    assertFalse(resultado.isPrincipal());
  }

  @Test
  void deveFalharComIdentificacaoVazia() {
    final Usuario gestor = criarGestor();
    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));

    final var input =
        new AdicionarFotoGaleriaUseCase.Input(
            UUID.randomUUID(),
            " ",
            "nova.jpg",
            "image/jpeg",
            512L,
            new ByteArrayInputStream(new byte[0]),
            gestor.getId());

    assertThrows(ValidationException.class, () -> useCase.upload(input));
  }

  @Test
  void deveFalharComTipoDeArquivoNaoPermitido() {
    final Usuario gestor = criarGestor();
    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));

    final var input =
        new AdicionarFotoGaleriaUseCase.Input(
            UUID.randomUUID(),
            "Parte 1",
            "malware.exe",
            "application/octet-stream",
            512L,
            new ByteArrayInputStream(new byte[0]),
            gestor.getId());

    assertThrows(ValidationException.class, () -> useCase.upload(input));
  }

  @Test
  void deveFalharComArquivoMuitoGrande() {
    final Usuario gestor = criarGestor();
    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));

    final var input =
        new AdicionarFotoGaleriaUseCase.Input(
            UUID.randomUUID(),
            "Parte 1",
            "grande.jpg",
            "image/jpeg",
            11L * 1024 * 1024,
            new ByteArrayInputStream(new byte[0]),
            gestor.getId());

    assertThrows(ValidationException.class, () -> useCase.upload(input));
  }

  @Test
  void operadorNaoDeveAdicionarFoto() {
    final Instant agora = Instant.now();
    final Usuario operador =
        new Usuario(
            UUID.randomUUID(),
            "Op",
            "op@test.com",
            "hash",
            PerfilUsuario.OPERADOR,
            true,
            agora,
            agora);

    when(usuarioRepository.findById(operador.getId())).thenReturn(Optional.of(operador));

    final var input =
        new AdicionarFotoGaleriaUseCase.Input(
            UUID.randomUUID(),
            "Parte 1",
            "f.jpg",
            "image/jpeg",
            100L,
            new ByteArrayInputStream(new byte[0]),
            operador.getId());

    assertThrows(NaoAutorizadoException.class, () -> useCase.upload(input));
  }
}
