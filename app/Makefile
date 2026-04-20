# ============================================================================
# Makefile para API do Canal de Cortes - Java Spring Boot
# ============================================================================

# Variáveis
MAVEN = ./mvnw
JAVA = java
JAR_FILE = target/api-0.0.1-SNAPSHOT.jar
COVERAGE_REPORT = target/site/jacoco/index.html

# Cores para output
RED = \033[0;31m
GREEN = \033[0;32m
YELLOW = \033[1;33m
BLUE = \033[0;34m
NC = \033[0m

.PHONY: help all clean build package run test ct test-component it test-integration verify lint format format-check coverage validate docker-build docker-up docker-down docker-logs docker-restart

help: ## Mostra esta ajuda
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "$(GREEN)%-20s$(NC) %s\n", $$1, $$2}'

all: clean build test coverage ## Build completo

clean: ## Limpa os artefatos de build
	@echo "$(BLUE)🧹 Limpando o projeto...$(NC)"
	$(MAVEN) clean
	@echo "$(GREEN)✅ Limpeza concluída!$(NC)"

build: ## Compila o projeto
	@echo "$(BLUE)🔨 Compilando o projeto...$(NC)"
	$(MAVEN) compile
	@echo "$(GREEN)✅ Compilação concluída!$(NC)"

package: ## Cria o JAR executável
	@echo "$(BLUE)📦 Criando JAR executável...$(NC)"
	$(MAVEN) package -DskipTests
	@echo "$(GREEN)✅ JAR criado!$(NC)"

run: ## Executa a aplicação Spring Boot
	@echo "$(BLUE)🚀 Iniciando a API...$(NC)"
	$(MAVEN) spring-boot:run

test: ## Executa testes unitários
	@echo "$(BLUE)🧪 Executando testes unitários...$(NC)"
	$(MAVEN) test
	@echo "$(GREEN)✅ Testes unitários concluídos!$(NC)"

ct: test-component ## Executa testes de componente (alias)

test-component: ## Executa testes de componente (WireMock)
	@echo "$(BLUE)🧩 Executando testes de componente...$(NC)"
	$(MAVEN) failsafe:integration-test -Dit.test=*CT -Dfailsafe.failIfNoSpecifiedTests=false failsafe:verify
	@echo "$(GREEN)✅ Testes de componente concluídos!$(NC)"

it: test-integration ## Executa testes de integração (alias)

test-integration: ## Executa testes de integração (Real Services)
	@echo "$(BLUE)🔗 Executando testes de integração...$(NC)"
	$(MAVEN) failsafe:integration-test -Dit.test=*IT -Dfailsafe.failIfNoSpecifiedTests=false failsafe:verify
	@echo "$(GREEN)✅ Testes de integração concluídos!$(NC)"

verify: ## Executa todos os testes (unitários, componente e integração)
	@echo "$(BLUE)🧪🧩🔗 Executando todos os testes...$(NC)"
	$(MAVEN) verify
	@echo "$(GREEN)✅ Todos os testes concluídos!$(NC)"

lint: ## Executa análise estática de código
	@echo "$(BLUE)🔍 Executando lint (Checkstyle)...$(NC)"
	$(MAVEN) checkstyle:check
	@echo "$(GREEN)✅ Lint concluído!$(NC)"

format: ## Formata o código fonte
	@echo "$(BLUE)🎨 Formatando código (Spotless)...$(NC)"
	$(MAVEN) spotless:apply
	@echo "$(GREEN)✅ Formatação concluída!$(NC)"

format-check: ## Valida se o código está formatado
	@echo "$(BLUE)📏 Validando formato do código...$(NC)"
	$(MAVEN) spotless:check
	@echo "$(GREEN)✅ Validação de formato concluída!$(NC)"

coverage: ## Gera relatório de cobertura
	@echo "$(BLUE)📊 Gerando relatório de cobertura...$(NC)"
	$(MAVEN) jacoco:report
	@echo "$(GREEN)✅ Relatório gerado em $(COVERAGE_REPORT)$(NC)"

validate: clean lint format-check test test-component coverage ## Executa lint, valida formato, testes e cobertura (>90%)
	@echo "$(BLUE)🛡️ Validando cobertura mínima (90%)...$(NC)"
	$(MAVEN) jacoco:check
	@echo "$(GREEN)✅ Validação completa concluída com sucesso!$(NC)"

# ============================================================================
# Docker
# ============================================================================

docker-build: ## Build da imagem Docker
	@echo "$(BLUE)🐳 Criando imagem Docker...$(NC)"
	docker compose build
	@echo "$(GREEN)✅ Imagem Docker criada!$(NC)"

	docker-up: ## Sobe a aplicação via Docker Compose
	@echo "$(BLUE)🚀 Subindo aplicação via Docker...$(NC)"
	docker compose up -d
	@echo "$(GREEN)✅ Aplicação rodando!$(NC)"

	docker-down: ## Para a aplicação via Docker Compose
	@echo "$(BLUE)🛑 Parando aplicação Docker...$(NC)"
	docker compose down
	@echo "$(GREEN)✅ Aplicação parada!$(NC)"

	docker-logs: ## Mostra logs da aplicação Docker
	docker compose logs -f

	docker-restart: docker-down docker-up ## Reinicia a aplicação Docker
