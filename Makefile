.PHONY: help setup build test test-fast test-all lint format run clean docker-up docker-down docker-logs check

help: ## Mostrar ajuda
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

# ── Setup ──────────────────────────────────────────────────

setup: ## Configurar ambiente (copiar .env, subir Docker, compilar)
	@test -f .env || cp .env.example .env
	$(MAKE) docker-up
	$(MAKE) build

# ── Build ──────────────────────────────────────────────────

build: ## Compilar o projeto (sem rodar testes)
	cd app && ./mvnw compile -q

format: ## Formatar codigo com Spotless
	cd app && ./mvnw spotless:apply -q

lint: ## Verificar formatacao com Spotless
	cd app && ./mvnw spotless:check -q

# ── Testes ─────────────────────────────────────────────────

test: ## Rodar testes unitarios e de integracao (sem Testcontainers)
	cd app && ./mvnw test -Dtest='!FlywayMigrationTest'

test-fast: ## Rodar apenas testes unitarios (sem @WebMvcTest e Flyway)
	cd app && ./mvnw test -Dtest='!FlywayMigrationTest,!*ControllerTest'

test-all: ## Rodar TODOS os testes incluindo Flyway/Testcontainers (requer Docker)
	cd app && ./mvnw test

check: ## Formatar + compilar + rodar testes
	$(MAKE) format
	$(MAKE) build
	$(MAKE) test

# ── Execucao ───────────────────────────────────────────────

run: ## Subir a aplicacao Spring Boot (requer Docker rodando)
	cd app && ./mvnw spring-boot:run

# ── Docker ─────────────────────────────────────────────────

docker-up: ## Subir PostgreSQL + MinIO via Docker Compose
	docker compose up -d

docker-down: ## Parar e remover containers
	docker compose down

docker-logs: ## Ver logs dos containers
	docker compose logs -f

docker-reset: ## Destruir volumes e recriar containers
	docker compose down -v
	docker compose up -d

# ── Limpeza ────────────────────────────────────────────────

clean: ## Limpar artefatos de build
	cd app && ./mvnw clean -q
