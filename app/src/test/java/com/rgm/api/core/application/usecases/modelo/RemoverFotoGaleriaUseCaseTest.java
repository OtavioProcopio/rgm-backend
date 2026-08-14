package com.rgm.api.core.application.usecases.modelo;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.model.aggregates.FotoGaleriaModelo;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.FotoGaleriaModeloRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import com.rgm.api.core.domain.ports.services.StorageService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RemoverFotoGaleriaUseCaseTest {

  private FotoGaleriaModeloRepository fotoGaleriaModeloRepository;
  private UsuarioRepository usuarioRepository;
  private StorageService storageService;
  private RemoverFotoGaleriaUseCase useCase;

  @BeforeEach
  void setUp() {
    fotoGaleriaModeloRepository = mock(FotoGaleriaModeloRepository.class);
    usuarioRepository = mock(UsuarioRepository.class);
    storageService = mock(StorageService.class);
    useCase =
        new RemoverFotoGaleriaUseCase(
            fotoGaleriaModeloRepository, usuarioRepository, storageService);
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

  @Test
  void deveRemoverFotoDaGaleria() {
    final Usuario gestor = criarGestor();
    final UUID modeloId = UUID.randomUUID();
    final FotoGaleriaModelo foto =
        FotoGaleriaModelo.criar(
            modeloId, "http://minio/foto.jpg", "Parte 1", true, gestor.getId(), Instant.now());

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(fotoGaleriaModeloRepository.findById(foto.getId())).thenReturn(Optional.of(foto));

    useCase.execute(new RemoverFotoGaleriaUseCase.Input(modeloId, foto.getId(), gestor.getId()));

    verify(storageService).delete("http://minio/foto.jpg");
    verify(fotoGaleriaModeloRepository).deleteById(foto.getId());
  }

  @Test
  void deveFalharQuandoFotoNaoPertenceAoModelo() {
    final Usuario gestor = criarGestor();
    final FotoGaleriaModelo foto =
        FotoGaleriaModelo.criar(
            UUID.randomUUID(),
            "http://minio/foto.jpg",
            "Parte 1",
            true,
            gestor.getId(),
            Instant.now());

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(fotoGaleriaModeloRepository.findById(foto.getId())).thenReturn(Optional.of(foto));

    assertThrows(
        BusinessRuleException.class,
        () ->
            useCase.execute(
                new RemoverFotoGaleriaUseCase.Input(
                    UUID.randomUUID(), foto.getId(), gestor.getId())));
  }

  @Test
  void operadorNaoDeveRemover() {
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

    assertThrows(
        NaoAutorizadoException.class,
        () ->
            useCase.execute(
                new RemoverFotoGaleriaUseCase.Input(
                    UUID.randomUUID(), UUID.randomUUID(), operador.getId())));
  }
}
