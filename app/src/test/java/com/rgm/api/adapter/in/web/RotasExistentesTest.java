package com.rgm.api.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

class RotasExistentesTest {

  private static final String PACOTE_WEB = "com.rgm.api.adapter.in.web";

  private static final List<String> ROTAS_DE_PARTIDA =
      List.of(
          "DELETE /api/admin/registros",
          "DELETE /api/modelos/{modeloId}/galeria/{fotoId}",
          "DELETE /api/solicitacoes/{solicitacaoId}/evidencias/{evidenciaId}",
          "GET /api/admin/usuarios",
          "GET /api/admin/usuarios/{id}",
          "GET /api/maquinas",
          "GET /api/modelos",
          "GET /api/modelos/relatorio",
          "GET /api/modelos/resumo",
          "GET /api/modelos/{id}",
          "GET /api/modelos/{id}/eventos",
          "GET /api/modelos/{id}/relatorio",
          "GET /api/modelos/{id}/solicitacoes/resumo",
          "GET /api/modelos/{modeloId}/galeria",
          "GET /api/solicitacoes",
          "GET /api/solicitacoes/events",
          "GET /api/solicitacoes/metricas",
          "GET /api/solicitacoes/metricas/historico",
          "GET /api/solicitacoes/metricas/por-modelo",
          "GET /api/solicitacoes/metricas/por-modelo/pdf",
          "GET /api/solicitacoes/relatorio",
          "GET /api/solicitacoes/{id}",
          "GET /api/solicitacoes/{id}/atividades",
          "GET /api/solicitacoes/{solicitacaoId}/evidencias",
          "GET /api/usuarios/me",
          "PATCH /api/admin/maquinas/{id}/ativar",
          "PATCH /api/admin/maquinas/{id}/desativar",
          "PATCH /api/admin/usuarios/{id}/ativar",
          "PATCH /api/admin/usuarios/{id}/desativar",
          "PATCH /api/admin/usuarios/{id}/perfil",
          "PATCH /api/admin/usuarios/{id}/senha",
          "PATCH /api/modelos/{id}/ativar",
          "PATCH /api/modelos/{id}/desativar",
          "PATCH /api/modelos/{modeloId}/galeria/{fotoId}",
          "PATCH /api/solicitacoes/{id}/cancelar",
          "PATCH /api/solicitacoes/{id}/devolver",
          "PATCH /api/solicitacoes/{id}/encerrar",
          "PATCH /api/solicitacoes/{id}/enviar-validacao",
          "PATCH /api/solicitacoes/{id}/responsaveis",
          "PATCH /api/solicitacoes/{id}/triar",
          "PATCH /api/usuarios/me/senha",
          "POST /api/admin/maquinas",
          "POST /api/admin/usuarios",
          "POST /api/auth/login",
          "POST /api/auth/refresh",
          "POST /api/modelos",
          "POST /api/modelos/{modeloId}/galeria",
          "POST /api/solicitacoes",
          "POST /api/solicitacoes/{id}/comentarios",
          "POST /api/solicitacoes/{solicitacaoId}/evidencias",
          "PUT /api/admin/maquinas/{id}",
          "PUT /api/admin/usuarios/{id}",
          "PUT /api/modelos/{id}",
          "PUT /api/solicitacoes/{id}");

  @Test
  void shouldKeepAllStartingRoutesMappedWhenControllersAreScanned() throws Exception {
    // Arrange
    Set<String> esperadas = new TreeSet<>(ROTAS_DE_PARTIDA);

    // Act
    Set<String> descobertas = descobrirRotas();
    esperadas.removeAll(descobertas);

    // Assert
    assertThat(esperadas)
        .as("Rotas de partida ausentes (nenhuma rota pode sumir ou ser renomeada)")
        .isEmpty();
  }

  @Test
  void shouldDiscoverAtLeastAsManyRoutesAsStartingListWhenControllersAreScanned() throws Exception {
    // Arrange
    final int minimoEsperado = ROTAS_DE_PARTIDA.size();

    // Act
    final Set<String> descobertas = descobrirRotas();

    // Assert
    assertThat(descobertas)
        .as("Rotas descobertas por reflexao (o scanner deve enxergar ao menos a lista de partida)")
        .hasSizeGreaterThanOrEqualTo(minimoEsperado);
  }

  private static Set<String> descobrirRotas() throws ClassNotFoundException {
    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
    Set<String> rotas = new TreeSet<>();
    for (BeanDefinition bean : scanner.findCandidateComponents(PACOTE_WEB)) {
      rotas.addAll(rotasDaClasse(Class.forName(bean.getBeanClassName())));
    }
    return rotas;
  }

  private static Set<String> rotasDaClasse(final Class<?> classe) {
    RequestMapping prefixo =
        AnnotatedElementUtils.findMergedAnnotation(classe, RequestMapping.class);
    String base = prefixo == null || prefixo.path().length == 0 ? "" : prefixo.path()[0];
    Set<String> rotas = new TreeSet<>();
    for (Method metodo : classe.getDeclaredMethods()) {
      rotas.addAll(rotasDoMetodo(base, metodo));
    }
    return rotas;
  }

  private static Set<String> rotasDoMetodo(final String base, final Method metodo) {
    RequestMapping mapeamento =
        AnnotatedElementUtils.findMergedAnnotation(metodo, RequestMapping.class);
    Set<String> rotas = new TreeSet<>();
    if (mapeamento == null) {
      return rotas;
    }
    List<String> caminhos =
        mapeamento.path().length == 0 ? List.of("") : Arrays.asList(mapeamento.path());
    for (RequestMethod verbo : mapeamento.method()) {
      caminhos.forEach(c -> rotas.add(verbo.name() + " " + normalizar(base + "/" + c)));
    }
    return rotas;
  }

  private static String normalizar(final String caminho) {
    String limpo = caminho.replaceAll("/+", "/");
    return limpo.length() > 1 && limpo.endsWith("/")
        ? limpo.substring(0, limpo.length() - 1)
        : limpo;
  }
}
