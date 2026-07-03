# RGM Backend

API REST do sistema de gerenciamento de solicitações de manutenção industrial.

**Stack:** Java 21 · Spring Boot 3.5 · PostgreSQL 16 · Flyway · MinIO · JWT · Docker

## Arquitetura

Clean Architecture com 3 camadas:

```
core/domain/        → Entidades, enums, exceções (0 deps de framework)
core/application/   → Use cases, ports (interfaces de repositório/serviço)
adapter/            → Controllers REST, JPA, MinIO, Security, Config
```

## Quick Start

```bash
cp .env.example .env
make setup && make run
```

Ou manualmente:

```bash
docker compose up -d
cd app && ./mvnw spring-boot:run
```

**URLs locais:**

| Serviço | URL |
|---------|-----|
| API | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |
| MinIO Console | http://localhost:9001 |

Login padrão (perfil `dev`): `admin@rgm.com` / `admin123`

## Testes

```bash
make check      # format + build + test (ciclo rápido)
make validate   # lint + testes + coverage 95% linhas + build (pipeline completo)
```

## 📚 Documentação

| | |
|---|---|
| 📋 [Casos de Uso](docs/casos-de-uso.md) | Todos os 15 UCs, regras de negócio e endpoints |
| 🗄️ [Modelo de Dados](docs/modelo_dados.md) | Entidades, enums e relacionamentos |
| 📊 [Diagramas](docs/diagramas.md) | Fluxos de uso, classes e sequências (Mermaid) |
| ⚙️ [Configuração](docs/configuracao.md) | Variáveis de ambiente, Makefile e Docker |
