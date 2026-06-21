## Tipo de mudança

- [ ] `feat` — Nova funcionalidade
- [ ] `fix` — Correção de bug
- [ ] `chore` — Configuração, dependências, manutenção
- [ ] `refactor` — Refatoração sem mudança de comportamento
- [ ] `test` — Adição ou correção de testes
- [ ] `docs` — Documentação
- [ ] `perf` — Melhoria de performance
- [ ] `hotfix` — Correção urgente em produção

---

## O que foi feito?

<!-- Descreva a mudança de forma clara e objetiva. -->

---

## Como testar?

<!-- Passos para validar manualmente ou rodar os testes relevantes. -->

```bash
# Exemplo:
make test-backend
# ou: acessar http://localhost:8080/swagger-ui.html → endpoint X
```

---

## Checklist

- [ ] CI passando (build + spotless + testes)
- [ ] Testes adicionados ou atualizados
- [ ] Sem breaking changes na API (ou documentado no body)
- [ ] Variáveis de ambiente novas adicionadas ao `.env.example`
- [ ] Migration Flyway criada se houver mudança no schema
