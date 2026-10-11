package com.rgm.api.adapter.config;

import java.time.DateTimeException;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Dados da fabrica lidos da configuracao, como o fuso em que o dia de trabalho e contado. */
@Configuration
public class FabricaConfig {

  private static final String FUSO_PADRAO = "America/Sao_Paulo";

  /** Fuso da fabrica (`rgm.fabrica.fuso`); valor invalido derruba a subida com a mensagem. */
  @Bean
  public ZoneId fusoDaFabrica(@Value("${rgm.fabrica.fuso:" + FUSO_PADRAO + "}") final String fuso) {
    try {
      return ZoneId.of(fuso);
    } catch (final DateTimeException e) {
      throw new IllegalStateException(
          "Propriedade 'rgm.fabrica.fuso' inválida (recebido: "
              + fuso
              + "). Formato esperado: identificador de fuso IANA, por exemplo "
              + FUSO_PADRAO,
          e);
    }
  }
}
