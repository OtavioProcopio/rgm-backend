package com.rgm.api.adapter.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ConfiguracaoDeProducaoTest {

  private static final String SWAGGER_UI = "springdoc.swagger-ui.enabled";
  private static final String API_DOCS = "springdoc.api-docs.enabled";

  private Properties carregar(final String arquivo) throws IOException {
    Properties propriedades = new Properties();
    try (InputStream entrada = new ClassPathResource(arquivo).getInputStream()) {
      propriedades.load(entrada);
    }
    return propriedades;
  }

  @Test
  void shouldDisableSwaggerUiWhenProfileIsProduction() throws IOException {
    // Arrange
    Properties producao = carregar("application-prod.properties");

    // Act
    String valor = producao.getProperty(SWAGGER_UI);

    // Assert
    assertEquals(
        "false", valor, "Esperado '" + SWAGGER_UI + "=false' em producao, recebido: " + valor);
  }

  @Test
  void shouldDisableApiDocsWhenProfileIsProduction() throws IOException {
    // Arrange
    Properties producao = carregar("application-prod.properties");

    // Act
    String valor = producao.getProperty(API_DOCS);

    // Assert
    assertEquals(
        "false", valor, "Esperado '" + API_DOCS + "=false' em producao, recebido: " + valor);
  }

  @Test
  void shouldDeclararOFusoDaFabricaWhenPropriedadesBase() throws IOException {
    // Arrange
    Properties base = carregar("application.properties");

    // Act
    String valor = base.getProperty("rgm.fabrica.fuso");

    // Assert
    assertEquals(
        "America/Sao_Paulo",
        valor,
        "Esperado 'rgm.fabrica.fuso=America/Sao_Paulo' no application.properties, recebido: "
            + valor);
  }

  @Test
  void shouldEnableSwaggerUiWhenProfileIsDevelopment() throws IOException {
    // Arrange
    Properties desenvolvimento = carregar("application-dev.properties");

    // Act
    String valor = desenvolvimento.getProperty(SWAGGER_UI);

    // Assert
    assertEquals(
        "true", valor, "Esperado '" + SWAGGER_UI + "=true' em desenvolvimento, recebido: " + valor);
  }
}
