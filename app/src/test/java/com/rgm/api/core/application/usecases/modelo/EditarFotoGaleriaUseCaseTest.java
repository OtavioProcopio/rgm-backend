package com.rgm.api.core.application.usecases.modelo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rgm.api.core.domain.exceptions.BusinessRuleException;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.aggregates.FotoGaleriaModelo;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.model.enums.PerfilUsuario;
import com.rgm.api.core.domain.ports.repositories.FotoGaleriaModeloRepository;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EditarFotoGaleriaUseCaseTest {

  private FotoGaleriaModeloRepository fotoGaleriaModeloRepository;
  private UsuarioRepository usuarioRepository;
  private EditarFotoGaleriaUseCase useCase;

  @BeforeEach
  void setUp() {
    fotoGaleriaModeloRepository = mock(FotoGaleriaModeloRepository.class);
    usuarioRepository = mock(UsuarioRepository.class);
    useCase = new EditarFotoGaleriaUseCase(fotoGaleriaModeloRepository, usuarioRepository);
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

  private FotoGaleriaModelo criarFoto(final UUID modeloId) {
    return FotoGaleriaModelo.criar(
        modeloId, "http://x/1.jpg", "Parte 1", false, UUID.randomUUID(), Instant.now());
  }

  @Test
  void deveRenomearIdentificacao() {
    final Usuario gestor = criarGestor();
    final UUID modeloId = UUID.randomUUID();
    final FotoGaleriaModelo foto = criarFoto(modeloId);

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(fotoGaleriaModeloRepository.findById(foto.getId())).thenReturn(Optional.of(foto));
    when(fotoGaleriaModeloRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final FotoGaleriaModelo resultado =
        useCase.execute(
            new EditarFotoGaleriaUseCase.Input(
                modeloId, foto.getId(), "Contra-macho", null, gestor.getId()));

    assertEquals("Contra-macho", resultado.getIdentificacao());
  }

  @Test
  void deveMarcarComoPrincipalELimparAAnterior() {
    final Usuario gestor = criarGestor();
    final UUID modeloId = UUID.randomUUID();
    final FotoGaleriaModelo foto = criarFoto(modeloId);

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(fotoGaleriaModeloRepository.findById(foto.getId())).thenReturn(Optional.of(foto));
    when(fotoGaleriaModeloRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    final FotoGaleriaModelo resultado =
        useCase.execute(
            new EditarFotoGaleriaUseCase.Input(modeloId, foto.getId(), null, true, gestor.getId()));

    assertTrue(resultado.isPrincipal());
    verify(fotoGaleriaModeloRepository).limparPrincipal(modeloId);
  }

  @Test
  void deveFalharComIdentificacaoVazia() {
    final Usuario gestor = criarGestor();
    final UUID modeloId = UUID.randomUUID();
    final FotoGaleriaModelo foto = criarFoto(modeloId);

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(fotoGaleriaModeloRepository.findById(foto.getId())).thenReturn(Optional.of(foto));

    assertThrows(
        ValidationException.class,
        () ->
            useCase.execute(
                new EditarFotoGaleriaUseCase.Input(
                    modeloId, foto.getId(), "  ", null, gestor.getId())));
  }

  @Test
  void deveFalharQuandoFotoNaoPertenceAoModelo() {
    final Usuario gestor = criarGestor();
    final FotoGaleriaModelo foto = criarFoto(UUID.randomUUID());

    when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));
    when(fotoGaleriaModeloRepository.findById(foto.getId())).thenReturn(Optional.of(foto));

    assertThrows(
        BusinessRuleException.class,
        () ->
            useCase.execute(
                new EditarFotoGaleriaUseCase.Input(
                    UUID.randomUUID(), foto.getId(), "Novo nome", null, gestor.getId())));
  }

  @Test
  void operadorNaoDeveEditar() {
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
                new EditarFotoGaleriaUseCase.Input(
                    UUID.randomUUID(), UUID.randomUUID(), "Novo", null, operador.getId())));
  }
}
