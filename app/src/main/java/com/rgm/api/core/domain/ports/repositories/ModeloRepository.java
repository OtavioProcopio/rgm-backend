package com.rgm.api.core.domain.ports.repositories;

import com.rgm.api.core.domain.model.aggregates.Modelo;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModeloRepository {

  Optional<Modelo> findById(UUID id);

  Modelo save(Modelo modelo);

  void deleteById(UUID id);

  /** Maior versao ja usada pelo codigo na maquina; 0 quando nao ha nenhum modelo. */
  int maiorVersao(String maquina, String codigo);

  /** Existe outro modelo (id diferente de {@code idDoModelo}) com a mesma identificacao. */
  boolean existeOutroComIdentificacao(String maquina, String codigo, int versao, UUID idDoModelo);

  /** Troca o nome da maquina em todos os modelos que a usam (renomeacao no catalogo). */
  int trocarMaquina(String de, String para);

  /** Troca o nome do tipo em todos os modelos que o usam (renomeacao no catalogo). */
  int trocarTipo(String de, String para);

  List<Modelo> findAllByIdIn(Collection<UUID> ids);

  PageResult<Modelo> findAll(int page, int size);

  PageResult<Modelo> findByFilters(
      Boolean ativo, String codigo, String maquina, String descricao, int page, int size);

  long count();

  /** Contagens do cadastro, agregadas no banco - nunca carrega a lista de modelos. */
  ResumoModelos resumir();
}
