package com.rgm.api.adapter.in.web;

import com.rgm.api.core.domain.exceptions.ValidationException;
import com.rgm.api.core.domain.model.enums.CriterioOrdenacaoSolicitacao;
import com.rgm.api.core.domain.ports.repositories.OrdenacaoSolicitacao;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Leitura dos parametros de consulta na borda web: pagina, data e enumeracao. Valor invalido vira
 * {@link ValidationException} (400) com o nome do campo, e nunca chega ao caso de uso.
 */
public final class ParametrosDeConsulta {

  public static final int TAMANHO_MAXIMO_DA_PAGINA = 100;
  public static final int DIAS_MAXIMOS_DO_HISTORICO = 365;
  public static final List<Integer> PERIODOS_DO_PAINEL = List.of(7, 30, 90);
  private static final int TAMANHO_MAXIMO_DO_VALOR_ECOADO = 50;

  private static final Map<String, CriterioOrdenacaoSolicitacao> CRITERIOS_DE_ORDENACAO =
      new LinkedHashMap<>();
  private static final Map<String, Boolean> SENTIDOS_DE_ORDENACAO = new LinkedHashMap<>();

  static {
    CRITERIOS_DE_ORDENACAO.put("criadaEm", CriterioOrdenacaoSolicitacao.CRIADA_EM);
    CRITERIOS_DE_ORDENACAO.put("prazoLimite", CriterioOrdenacaoSolicitacao.PRAZO_LIMITE);
    CRITERIOS_DE_ORDENACAO.put("prioridade", CriterioOrdenacaoSolicitacao.PRIORIDADE);
    SENTIDOS_DE_ORDENACAO.put("asc", true);
    SENTIDOS_DE_ORDENACAO.put("desc", false);
  }

  private ParametrosDeConsulta() {}

  /** {@code page} a partir de 0 e {@code size} de 1 ao maximo da pagina. */
  public static void pagina(final int page, final int size) {
    if (page < 0) {
      throw new ValidationException(
          "Parâmetro 'page' deve ser maior ou igual a 0 (recebido: " + page + ")");
    }
    if (size < 1 || size > TAMANHO_MAXIMO_DA_PAGINA) {
      throw new ValidationException(
          "Parâmetro 'size' deve estar entre 1 e "
              + TAMANHO_MAXIMO_DA_PAGINA
              + " (recebido: "
              + size
              + ")");
    }
  }

  /** {@code dias} do historico de metricas: de 1 ao maximo de 365. */
  public static void diasDoHistorico(final int dias) {
    if (dias < 1 || dias > DIAS_MAXIMOS_DO_HISTORICO) {
      throw new ValidationException(
          "Parâmetro 'dias' deve estar entre 1 e "
              + DIAS_MAXIMOS_DO_HISTORICO
              + " (recebido: "
              + dias
              + ")");
    }
  }

  /** {@code dias} do painel por periodo: so 7, 30 ou 90. */
  public static void periodoDoPainel(final int dias) {
    if (!PERIODOS_DO_PAINEL.contains(dias)) {
      throw new ValidationException(
          "Parâmetro 'dias' inválido (recebido: "
              + dias
              + "). Valores aceitos: "
              + PERIODOS_DO_PAINEL.stream().map(String::valueOf).collect(Collectors.joining(", ")));
    }
  }

  /** Valor devolvido na mensagem de erro, limitado para nao ecoar entrada longa. */
  private static String recebido(final String valor) {
    return valor.length() > TAMANHO_MAXIMO_DO_VALOR_ECOADO
        ? valor.substring(0, TAMANHO_MAXIMO_DO_VALOR_ECOADO) + "…"
        : valor;
  }

  /** Data em ISO-8601 com fuso; {@code null} quando o parametro nao foi informado. */
  public static Instant data(final String campo, final String valor) {
    if (valor == null) {
      return null;
    }
    try {
      return Instant.parse(valor);
    } catch (final DateTimeParseException e) {
      throw new ValidationException(
          "Parâmetro '"
              + campo
              + "' deve estar no formato ISO-8601, por exemplo 2026-10-10T00:00:00Z"
              + " (recebido: "
              + recebido(valor)
              + ")");
    }
  }

  /**
   * Ordenacao da listagem: {@code sort} em criadaEm, prazoLimite ou prioridade; {@code dir} em asc
   * ou desc. Sem {@code sort} vale a criacao; sem {@code dir} vale o sentido crescente.
   */
  public static OrdenacaoSolicitacao ordenacao(final String sort, final String dir) {
    final CriterioOrdenacaoSolicitacao criterio =
        sort == null
            ? CriterioOrdenacaoSolicitacao.CRIADA_EM
            : valorDaLista("sort", sort, CRITERIOS_DE_ORDENACAO);
    final boolean ascendente = dir == null || valorDaLista("dir", dir, SENTIDOS_DE_ORDENACAO);
    return new OrdenacaoSolicitacao(criterio, ascendente);
  }

  /** Valor mapeado na lista fechada do parametro; fora dela, 400 com os valores aceitos. */
  private static <V> V valorDaLista(
      final String campo, final String valor, final Map<String, V> aceitos) {
    final V resultado = aceitos.get(valor);
    if (resultado == null) {
      throw new ValidationException(
          "Parâmetro '"
              + campo
              + "' inválido (recebido: "
              + recebido(valor)
              + "). Valores aceitos: "
              + String.join(", ", aceitos.keySet()));
    }
    return resultado;
  }

  /** Constante da enumeracao; {@code null} quando o parametro nao foi informado. */
  public static <E extends Enum<E>> E enumeracao(
      final String campo, final String valor, final Class<E> tipo) {
    if (valor == null) {
      return null;
    }
    try {
      return Enum.valueOf(tipo, valor);
    } catch (final IllegalArgumentException e) {
      final String aceitos =
          Arrays.stream(tipo.getEnumConstants()).map(Enum::name).collect(Collectors.joining(", "));
      throw new ValidationException(
          "Parâmetro '"
              + campo
              + "' inválido (recebido: "
              + recebido(valor)
              + "). Valores aceitos: "
              + aceitos);
    }
  }
}
