package com.rgm.api.adapter.out.persistence;

import com.rgm.api.adapter.out.persistence.mapper.ModeloMapper;
import com.rgm.api.adapter.out.persistence.repository.ModeloJpaRepository;
import com.rgm.api.core.domain.model.aggregates.Modelo;
import com.rgm.api.core.domain.ports.repositories.ContagensDeModelos;
import com.rgm.api.core.domain.ports.repositories.FiltrosDeModelos;
import com.rgm.api.core.domain.ports.repositories.ModeloRepository;
import com.rgm.api.core.domain.ports.repositories.OrdemDeModelos;
import com.rgm.api.core.domain.ports.repositories.PageResult;
import com.rgm.api.core.domain.ports.repositories.PosicaoDoModelo;
import com.rgm.api.core.domain.ports.repositories.QuantidadePorMaquina;
import com.rgm.api.core.domain.ports.repositories.ResumoModelos;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class ModeloRepositoryAdapter implements ModeloRepository {

  private final ModeloJpaRepository jpa;

  public ModeloRepositoryAdapter(final ModeloJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<Modelo> findById(final UUID id) {
    return jpa.findById(id).map(ModeloMapper::toDomain);
  }

  @Override
  public Modelo save(final Modelo modelo) {
    return ModeloMapper.toDomain(jpa.save(ModeloMapper.toJpa(modelo)));
  }

  @Override
  public void deleteById(final UUID id) {
    jpa.deleteById(id);
  }

  @Override
  public int maiorVersao(final String maquina, final String codigo) {
    return jpa.maiorVersao(maquina, codigo);
  }

  @Override
  public boolean existeOutroComIdentificacao(
      final String maquina, final String codigo, final int versao, final UUID idDoModelo) {
    return jpa.existsByMaquinaAndCodigoAndVersaoAndIdNot(maquina, codigo, versao, idDoModelo);
  }

  @Override
  public int trocarMaquina(final String de, final String para) {
    return jpa.trocarMaquina(de, para);
  }

  @Override
  public int trocarTipo(final String de, final String para) {
    return jpa.trocarTipo(de, para);
  }

  @Override
  public List<Modelo> findAllByIdIn(final Collection<UUID> ids) {
    return jpa.findAllById(ids).stream().map(ModeloMapper::toDomain).toList();
  }

  @Override
  public PageResult<Modelo> findAll(final int page, final int size) {
    final var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "criadoEm"));
    final var result = jpa.findAll(pageable);
    return new PageResult<>(
        result.getContent().stream().map(ModeloMapper::toDomain).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Override
  public PageResult<Modelo> findByFilters(
      final Boolean ativo,
      final String codigo,
      final String maquina,
      final String descricao,
      final int page,
      final int size) {
    final var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "criadoEm"));
    final var result = jpa.findByFilters(ativo, codigo, maquina, descricao, pageable);
    return new PageResult<>(
        result.getContent().stream().map(ModeloMapper::toDomain).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Override
  public PageResult<Modelo> buscarCatalogo(
      final FiltrosDeModelos f, final OrdemDeModelos ordem, final int page, final int size) {
    final var pageable = PageRequest.of(page, size);
    final String q = termoDeBusca(f);
    final var result =
        ordem == OrdemDeModelos.CODIGO
            ? jpa.catalogoPorCodigo(
                f.ativo(),
                f.codigo(),
                f.maquina(),
                f.maquinaExata(),
                f.descricao(),
                f.tipo(),
                q,
                pageable)
            : jpa.catalogoPorCriacao(
                f.ativo(),
                f.codigo(),
                f.maquina(),
                f.maquinaExata(),
                f.descricao(),
                f.tipo(),
                q,
                pageable);
    return new PageResult<>(
        result.getContent().stream().map(ModeloMapper::toDomain).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Override
  public ContagensDeModelos contar(final FiltrosDeModelos f) {
    final String q = termoDeBusca(f);
    final long total =
        jpa.contarNoCatalogo(
            f.ativo(), f.codigo(), f.maquina(), f.maquinaExata(), f.descricao(), f.tipo(), q);
    // Cada grupo ignora o proprio filtro: o botao mostra o que traria.
    final var semTipo = f.semTipo();
    final var semMaquina = f.semMaquina();
    return new ContagensDeModelos(
        total,
        quantidades(
            jpa.contarNoCatalogoPorTipo(
                semTipo.ativo(),
                semTipo.codigo(),
                semTipo.maquina(),
                semTipo.maquinaExata(),
                semTipo.descricao(),
                semTipo.tipo(),
                q)),
        quantidades(
            jpa.contarNoCatalogoPorMaquina(
                semMaquina.ativo(),
                semMaquina.codigo(),
                semMaquina.maquina(),
                semMaquina.maquinaExata(),
                semMaquina.descricao(),
                semMaquina.tipo(),
                q)));
  }

  @Override
  public PosicaoDoModelo posicaoDe(final UUID id, final FiltrosDeModelos f) {
    final String q = termoDeBusca(f);
    final long total =
        jpa.contarNoCatalogo(
            f.ativo(), f.codigo(), f.maquina(), f.maquinaExata(), f.descricao(), f.tipo(), q);
    if (jpa.contarModelo(
            f.ativo(), f.codigo(), f.maquina(), f.maquinaExata(), f.descricao(), f.tipo(), q, id)
        == 0) {
      return new PosicaoDoModelo(null, total);
    }
    final var alvo = jpa.findById(id).orElseThrow();
    final long antes =
        jpa.contarAntesDe(
            f.ativo(),
            f.codigo(),
            f.maquina(),
            f.maquinaExata(),
            f.descricao(),
            f.tipo(),
            q,
            alvo.getCodigo(),
            alvo.getVersao(),
            id);
    return new PosicaoDoModelo((int) antes + 1, total);
  }

  private static String termoDeBusca(final FiltrosDeModelos f) {
    return f.q() == null ? null : TextoDeBusca.escaparCuringas(f.q());
  }

  private static List<ContagensDeModelos.Quantidade> quantidades(final List<Object[]> linhas) {
    return linhas.stream()
        .map(l -> new ContagensDeModelos.Quantidade((String) l[0], ((Number) l[1]).longValue()))
        .toList();
  }

  @Override
  public long count() {
    return jpa.count();
  }

  @Override
  public ResumoModelos resumir() {
    final Object[] contagens = jpa.resumirContagens().get(0);
    final long total = ((Number) contagens[0]).longValue();
    final long ativos = ((Number) contagens[1]).longValue();
    final var porMaquina =
        jpa.contarPorMaquina().stream()
            .map(row -> new QuantidadePorMaquina((String) row[0], ((Number) row[1]).longValue()))
            .toList();
    return new ResumoModelos(
        total, ativos, total - ativos, ((Number) contagens[2]).longValue(), porMaquina);
  }
}
