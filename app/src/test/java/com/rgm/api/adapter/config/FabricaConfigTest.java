package com.rgm.api.adapter.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class FabricaConfigTest {

  private final ApplicationContextRunner contexto =
      new ApplicationContextRunner().withUserConfiguration(FabricaConfig.class);

  @Test
  void shouldUsarSaoPauloWhenFusoNaoEConfigurado() {
    // Arrange
    final ZoneId esperado = ZoneId.of("America/Sao_Paulo");

    // Act
    contexto.run(
        // Assert
        ctx -> assertEquals(esperado, ctx.getBean(ZoneId.class)));
  }

  @Test
  void shouldUsarOFusoConfiguradoWhenPropriedadeEPreenchida() {
    // Arrange
    final ZoneId esperado = ZoneId.of("America/Manaus");

    // Act
    contexto
        .withPropertyValues("rgm.fabrica.fuso=America/Manaus")
        .run(
            // Assert
            ctx -> assertEquals(esperado, ctx.getBean(ZoneId.class)));
  }

  @Test
  void shouldFalharNaSubidaWhenFusoEInvalido() {
    // Arrange
    final String invalido = "Marte/Olimpo";

    // Act
    contexto
        .withPropertyValues("rgm.fabrica.fuso=" + invalido)
        .run(
            // Assert
            ctx -> {
              assertNotNull(ctx.getStartupFailure());
              final String mensagens = mensagensDaCadeia(ctx.getStartupFailure());
              assertTrue(
                  mensagens.contains("(recebido: " + invalido + ")")
                      && mensagens.contains("por exemplo America/Sao_Paulo"),
                  "Mensagem sem o valor recebido e o formato esperado: " + mensagens);
            });
  }

  private static String mensagensDaCadeia(final Throwable erro) {
    final StringBuilder mensagens = new StringBuilder();
    for (Throwable atual = erro; atual != null; atual = atual.getCause()) {
      mensagens.append(atual.getMessage()).append(" | ");
    }
    return mensagens.toString();
  }
}
