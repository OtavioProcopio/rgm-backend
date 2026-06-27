# RGM Backend

API REST para o sistema RGM — gerenciamento de solicitações de manutenção, modelos de equipamento e evidências digitais.

## Stack

- Java 21 + Spring Boot 3.5
- PostgreSQL 16 + Flyway
- MinIO (armazenamento S3)
- JWT (access + refresh token)
- Docker + Docker Compose

## Arquitetura

Clean Architecture com 3 camadas:

```
core/domain/        → Entidades, enums, exceções (0 deps de framework)
core/application/   → Use cases, ports (interfaces de repositório/serviço)
adapter/            → Controllers REST, JPA, MinIO, Security, Config
```

## Quick Start

```bash
# 1. Configurar ambiente
cp .env.example .env

# 2. Subir PostgreSQL + MinIO
docker compose up -d

# 3. Rodar a aplicação
cd app && ./mvnw spring-boot:run
```

Ou via Makefile: `make setup && make run`

**URLs locais:**
| Serviço | URL |
|---------|-----|
| API | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |
| MinIO Console | http://localhost:9001 |

## Variáveis de Ambiente

| Variável | Default | Descrição |
|----------|---------|-----------|
| `DB_HOST` | `localhost` | Host do PostgreSQL |
| `DB_PORT` | `5432` | Porta do PostgreSQL |
| `DB_NAME` | `rgm` | Nome do banco |
| `DB_USER` | `postgres` | Usuário do banco |
| `DB_PASSWORD` | `postgres` | Senha do banco |
| `JWT_SECRET` | *(obrigatório)* | Chave HMAC ≥ 32 bytes |
| `JWT_EXPIRATION_HOURS` | `24` | Validade do access token |
| `JWT_REFRESH_EXPIRATION_DAYS` | `7` | Validade do refresh token |
| `MINIO_URL` | `http://localhost:9000` | URL do MinIO |
| `MINIO_ACCESS_KEY` | `root` | Access key do MinIO |
| `MINIO_SECRET_KEY` | `password123` | Secret key do MinIO |
| `MINIO_BUCKET_NAME` | `images` | Bucket para evidências |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173` | Origens CORS |
| `RATE_LIMIT_MAX_REQUESTS` | `10` | Máx. requests/janela no login |
| `RATE_LIMIT_WINDOW_SECONDS` | `60` | Janela do rate limiter (seg) |
| `SPRING_PROFILES_ACTIVE` | — | Usar `dev` para desenvolvimento |

O perfil `dev` habilita: admin seed (`admin@rgm.com` / `admin123`), SQL logging e JWT secret default.

## Endpoints

### Auth (`/api/auth`)

| Método | Path | Descrição |
|--------|------|-----------|
| POST | `/login` | Login (retorna access + refresh token) |
| POST | `/refresh` | Renovar tokens |

### Solicitações (`/api/solicitacoes`)

| Método | Path | Descrição |
|--------|------|-----------|
| POST | `/` | Abrir solicitação (UC-02) |
| GET | `/` | Listar solicitações (paginado) |
| GET | `/{id}` | Detalhe da solicitação |
| GET | `/{id}/atividades` | Histórico de atividades |
| PATCH | `/{id}/triar` | Triar e atribuir (UC-03) |
| PATCH | `/{id}/enviar-validacao` | Enviar para validação (UC-05) |
| PATCH | `/{id}/devolver` | Devolver para correção (UC-06) |
| PATCH | `/{id}/encerrar` | Concluir/cancelar (UC-07) |
| POST | `/{id}/comentarios` | Adicionar comentário |

### Evidências (`/api/solicitacoes/{solicitacaoId}/evidencias`)

| Método | Path | Descrição |
|--------|------|-----------|
| POST | `/` | Upload de evidência (UC-08) |
| GET | `/{id}` | Visualizar evidência (UC-09) |

### Modelos (`/api/modelos`)

| Método | Path | Descrição |
|--------|------|-----------|
| POST | `/` | Criar modelo |
| GET | `/` | Listar modelos (paginado) |
| GET | `/{id}` | Detalhe do modelo |
| GET | `/{id}/eventos` | Histórico de eventos |
| PUT | `/{id}` | Editar modelo |
| PATCH | `/{id}/desativar` | Desativar modelo |
| PATCH | `/{id}/foto-capa` | Atualizar foto de capa (UC-14) |

### Admin (`/api/admin`)

| Método | Path | Descrição |
|--------|------|-----------|
| POST | `/usuarios` | Criar usuário (inclui EXTERNO) |
| GET | `/usuarios` | Listar usuários (paginado) |
| GET | `/usuarios/{id}` | Detalhe do usuário |
| PATCH | `/usuarios/{id}/desativar` | Desativar usuário |
| PATCH | `/usuarios/{id}/ativar` | Ativar usuário |
| DELETE | `/registros` | Exclusão hard delete (UC-15) |

## Perfis de Usuário (RBAC)

| Perfil | Pode criar solicitação | Movimentar Kanban | Gerenciar modelos | Gerenciar usuários | Ser atribuído |
|--------|----------------------|-------------------|-------------------|---------------------------|--------------|
| ADMINISTRADOR | — | Qualquer | — | Sim | Não |
| GESTOR | Sim | Qualquer | Sim | — | Sim |
| OPERADOR | Sim | Apenas atribuídas | — | — | Sim |
| EXTERNO | — | — | — | — | Sim (passivo) |

## Testes

```bash
make test          # Unitários + integração (sem Testcontainers)
make test-fast     # Apenas unitários
make test-all      # Todos (requer Docker para Testcontainers)
make check         # Format + build + test
```

## Docker (Produção)

```bash
docker build -t rgm-backend .
docker run -p 8080:8080 \
  -e JWT_SECRET=sua-chave-secreta-com-pelo-menos-32-bytes \
  -e DB_HOST=seu-postgres \
  -e DB_PASSWORD=sua-senha \
  rgm-backend
```

## Casos de Uso Implementados

| UC | Descrição | Status |
|----|-----------|--------|
| UC-01 | Login | ✓ |
| UC-02 | Abrir solicitação | ✓ |
| UC-03 | Triar e atribuir | ✓ |
| UC-04 | Autorização de movimentação Kanban | ✓ |
| UC-05 | Enviar para validação | ✓ |
| UC-06 | Devolver para correção | ✓ |
| UC-07 | Encerrar solicitação | ✓ |
| UC-08 | Anexar evidência (upload) | ✓ |
| UC-09 | Visualizar evidência | ✓ |
| UC-10 | Recalcular pendência do modelo | ✓ |
| UC-11 | Cadastrar prestador externo | ✓ |
| UC-12 | Atribuir externo + movimentar | ✓ |
| UC-13 | Administração (CRUD) | ✓ |
| UC-14 | Atualizar foto de capa | ✓ |
| UC-15 | Exclusão hard delete | ✓ |

## Makefile

```bash
make help          # Ver todos os comandos disponíveis
make setup         # Setup completo (env + Docker + build)
make run           # Subir a aplicação
make docker-up     # Subir PostgreSQL + MinIO
make docker-down   # Parar containers
make format        # Formatar código (Spotless)
make lint          # Verificar formatação
```

## Licença

Uso interno.
