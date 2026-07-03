# Configuração — RGM Backend

## Variáveis de Ambiente

Copie `.env.example` para `.env` antes de iniciar.

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
| `MINIO_URL` | `http://localhost:9000` | URL interna do MinIO |
| `MINIO_PUBLIC_URL` | `http://localhost:9000` | URL pública para o browser |
| `MINIO_ACCESS_KEY` | `root` | Access key do MinIO |
| `MINIO_SECRET_KEY` | `password123` | Secret key do MinIO |
| `MINIO_BUCKET_NAME` | `images` | Bucket para evidências |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173` | Origens CORS |
| `RATE_LIMIT_MAX_REQUESTS` | `10` | Máx. requests/janela no login |
| `RATE_LIMIT_WINDOW_SECONDS` | `60` | Janela do rate limiter (seg) |
| `SPRING_PROFILES_ACTIVE` | — | Usar `dev` para desenvolvimento |

O perfil `dev` habilita: admin seed (`admin@rgm.com` / `admin123`), SQL logging e JWT secret default.

## Makefile

```bash
make help          # Ver todos os comandos disponíveis
make setup         # Setup completo (env + Docker + build)
make run           # Subir a aplicação
make docker-up     # Subir PostgreSQL + MinIO
make docker-down   # Parar containers
make format        # Formatar código (Spotless)
make lint          # Verificar formatação
make check         # Format + build + test (ciclo rápido)
make validate      # Lint + testes + coverage ≥95% linhas + build (pipeline completo)
make coverage      # Apenas relatório JaCoCo
```

## Perfis de Usuário (RBAC)

| Perfil | Pode criar solicitação | Movimentar Kanban | Gerenciar modelos | Gerenciar usuários | Ser atribuído |
|--------|----------------------|-------------------|-------------------|---------------------------|--------------|
| ADMINISTRADOR | — | Qualquer | — | Sim | Não |
| GESTOR | Sim | Qualquer | Sim | — | Sim |
| OPERADOR | Sim | Apenas atribuídas | — | — | Sim |
| EXTERNO | — | — | — | — | Sim (passivo) |

## Docker — Produção

As imagens de produção são publicadas no GitHub Container Registry (GHCR) pelo CI.

```bash
# Via rgm-infra (recomendado)
cd ../rgm-infra
make setup && make prod-up
```

Ou diretamente:

```bash
docker build -t rgm-backend .
docker run -p 8080:8080 \
  -e JWT_SECRET=sua-chave-secreta-com-pelo-menos-32-bytes \
  -e DB_HOST=seu-postgres \
  -e DB_PASSWORD=sua-senha \
  rgm-backend
```

> Ver `../rgm-infra/README.md` para orquestração completa (backend + frontend + banco + MinIO).
