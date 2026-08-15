# Autenticacao Specification

## Purpose

Autenticar usuarios internos (OPERADOR, GESTOR, ADMINISTRADOR) via login com
email/senha, emitir tokens JWT de acesso e refresh, e permitir que qualquer
usuario logado altere a propria senha. Usuarios do perfil EXTERNO nunca fazem
login (nao possuem credenciais).

## Requirements

### Requirement: Login com email e senha
O sistema SHALL autenticar um usuario a partir de email e senha, retornando um
access token JWT, um refresh token, o nome e o perfil do usuario.

#### Scenario: Login valido
- **WHEN** um usuario ativo com perfil OPERADOR, GESTOR ou ADMINISTRADOR envia
  email e senha corretos
- **THEN** o sistema retorna 200 com token, refreshToken, nome e perfil

#### Scenario: Senha incorreta
- **WHEN** a senha nao confere com o hash armazenado
- **THEN** o sistema retorna 401 com mensagem generica "Credenciais invalidas"
  (sem indicar se o email existe)

#### Scenario: Usuario inativo
- **WHEN** o usuario existe e a senha esta correta, mas `ativo = false`
- **THEN** o sistema retorna 401 "Usuario inativo"

#### Scenario: Perfil EXTERNO tenta logar
- **WHEN** o email pertence a um usuario de perfil EXTERNO
- **THEN** o sistema retorna 401 "Credenciais invalidas" (EXTERNO nunca tem
  senha, entao nunca autentica)

### Requirement: Renovacao de access token via refresh token
O sistema SHALL emitir um novo par de tokens (access + refresh) a partir de um
refresh token valido, sem exigir senha novamente.

#### Scenario: Refresh valido
- **WHEN** um refresh token valido e nao expirado e enviado
- **THEN** o sistema retorna um novo access token e um novo refresh token

#### Scenario: Refresh invalido, expirado ou de usuario inativo
- **WHEN** o refresh token e invalido/expirado, ou o usuario associado esta
  inativo, ou nao existe mais
- **THEN** o sistema retorna 401 "Refresh token invalido ou expirado" (ou
  "Usuario inativo"/"Usuario nao encontrado" conforme o caso)

### Requirement: Alterar a propria senha
O sistema SHALL permitir que qualquer usuario logado (exceto EXTERNO, que nao
possui senha) altere a propria senha, exigindo a senha atual correta.

#### Scenario: Troca de senha bem-sucedida
- **WHEN** um usuario interno logado informa a senha atual correta e uma nova
  senha nao vazia
- **THEN** o sistema atualiza o hash da senha e a proxima autenticacao usa a
  nova senha

#### Scenario: Senha atual incorreta
- **WHEN** a senha atual informada nao confere
- **THEN** o sistema rejeita com erro de regra de negocio, sem alterar nada

#### Scenario: Usuario EXTERNO tenta alterar senha
- **WHEN** o usuario autenticado tem perfil EXTERNO
- **THEN** o sistema rejeita com "Prestador externo nao possui senha"
