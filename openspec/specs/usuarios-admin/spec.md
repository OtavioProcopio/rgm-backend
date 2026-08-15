# Usuarios Admin Specification

## Purpose

Permitir que um ADMINISTRADOR cadastre, edite, ative/desative e gerencie
senhas e perfis de usuarios internos (OPERADOR, GESTOR, ADMINISTRADOR), alem
de cadastrar prestadores externos (perfil EXTERNO, sem login). Toda operacao
de gestao de usuarios e restrita a ADMINISTRADOR.

## Requirements

### Requirement: Somente ADMINISTRADOR gerencia usuarios
O sistema SHALL restringir toda criacao/edicao/ativacao/desativacao/redefinicao
de senha/alteracao de perfil de usuarios ao perfil ADMINISTRADOR.

#### Scenario: Usuario nao-administrador tenta gerenciar usuarios
- **WHEN** um usuario OPERADOR, GESTOR ou EXTERNO tenta executar qualquer
  operacao de gestao de usuarios
- **THEN** o sistema rejeita com erro de nao autorizado

### Requirement: Cadastrar usuario interno
O sistema SHALL permitir que um ADMINISTRADOR cadastre um novo usuario interno
com nome, email unico, senha e perfil (OPERADOR, GESTOR ou ADMINISTRADOR).

#### Scenario: Cadastro bem-sucedido
- **WHEN** um ADMINISTRADOR cadastra um usuario com email ainda nao usado
- **THEN** o usuario e criado ativo, com a senha armazenada como hash

#### Scenario: Email duplicado
- **WHEN** o email informado ja pertence a outro usuario
- **THEN** o sistema rejeita com "Email ja cadastrado"

#### Scenario: Tentativa de criar EXTERNO por este fluxo
- **WHEN** o perfil informado e EXTERNO
- **THEN** o sistema rejeita, orientando a usar o cadastro de prestador
  externo (fluxo dedicado, sem senha/email)

### Requirement: Cadastrar prestador externo
O sistema SHALL permitir que um ADMINISTRADOR cadastre um prestador externo
(perfil EXTERNO) informando apenas o nome — sem email nem senha, pois esse
perfil nunca faz login.

#### Scenario: Cadastro de prestador externo
- **WHEN** um ADMINISTRADOR informa apenas um nome
- **THEN** o sistema cria um usuario EXTERNO ativo, sem credenciais de login

### Requirement: Editar dados de usuario
O sistema SHALL permitir que um ADMINISTRADOR edite nome e email de um usuario
interno existente, mantendo o email unico no sistema.

#### Scenario: Edicao bem-sucedida
- **WHEN** o novo email nao pertence a outro usuario
- **THEN** nome e email sao atualizados

#### Scenario: Email ja usado por outro usuario
- **WHEN** o novo email ja pertence a outro usuario (diferente do editado)
- **THEN** o sistema rejeita com "Email ja cadastrado"

#### Scenario: Tentativa de editar usuario EXTERNO por este fluxo
- **WHEN** o usuario alvo tem perfil EXTERNO
- **THEN** o sistema rejeita a edicao por este caso de uso

### Requirement: Ativar e desativar usuario
O sistema SHALL permitir que um ADMINISTRADOR ative ou desative qualquer
usuario, exceto a propria conta (nao pode se autodesativar).

#### Scenario: Desativar outro usuario
- **WHEN** um ADMINISTRADOR desativa um usuario diferente de si mesmo
- **THEN** o usuario passa a `ativo = false` e nao consegue mais logar

#### Scenario: Tentativa de autodesativacao
- **WHEN** o ADMINISTRADOR tenta desativar a propria conta
- **THEN** o sistema rejeita com "Nao e possivel desativar a propria conta de
  administrador"

### Requirement: Redefinir senha de usuario
O sistema SHALL permitir que um ADMINISTRADOR redefina a senha de um usuario
interno (perfis com login), sem exigir a senha atual.

#### Scenario: Redefinicao bem-sucedida
- **WHEN** um ADMINISTRADOR informa uma nova senha nao vazia para um usuario
  interno
- **THEN** o hash da senha e atualizado

#### Scenario: Tentativa em usuario EXTERNO
- **WHEN** o usuario alvo tem perfil EXTERNO
- **THEN** o sistema rejeita, pois EXTERNO nao possui senha

### Requirement: Alterar perfil de usuario
O sistema SHALL permitir que um ADMINISTRADOR altere o perfil de um usuario
interno entre OPERADOR/GESTOR/ADMINISTRADOR, preservando a integridade do
perfil EXTERNO e a auto-protecao do proprio ADMINISTRADOR.

#### Scenario: Alteracao entre perfis internos
- **WHEN** um ADMINISTRADOR altera o perfil de outro usuario entre
  OPERADOR/GESTOR/ADMINISTRADOR
- **THEN** o novo perfil e aplicado

#### Scenario: Administrador tenta rebaixar o proprio perfil
- **WHEN** o ADMINISTRADOR tenta alterar o proprio perfil para algo diferente
  de ADMINISTRADOR
- **THEN** o sistema rejeita, evitando que ele perca acesso administrativo

#### Scenario: Tentativa de converter EXTERNO em interno ou vice-versa
- **WHEN** a alteracao tentaria transformar um usuario EXTERNO em interno, ou
  um usuario interno em EXTERNO
- **THEN** o sistema rejeita — a transicao entre EXTERNO e interno nao e
  suportada por este caso de uso
