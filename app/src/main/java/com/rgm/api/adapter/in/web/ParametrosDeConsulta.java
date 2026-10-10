package com.rgm.api.adapter.in.web;

import com.rgm.api.core.domain.exceptions.ValidationException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Leitura dos parametros de consulta na borda web: pagina, data e enumeracao. Valor invalido vira
 * {@link ValidationException} (400) com o nome do campo, e nunca chega ao caso de uso.
 */
public final class ParametrosDeConsulta {

  public static final int TAMANHO_MAXIMO_DA_PAGINA = 100;
  private static final int TAMANHO_MAXIMO_DO_VALOR_ECOADO = 50;

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
