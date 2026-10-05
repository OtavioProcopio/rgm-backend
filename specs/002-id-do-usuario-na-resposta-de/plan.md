# Plano de implementação — Id do usuário na resposta de login

> Descreve **como**. Deriva da spec e da constituição; não introduz requisito novo.

Caminhos relativos a `app/src/main/java/com/rgm/api/` (produção) e
`app/src/test/java/com/rgm/api/` (teste), conforme o Princípio 7.

## Decisões técnicas

| Decisão | Escolha | Alternativas descartadas | Por quê |
|---|---|---|---|
| Tipo do identificador na saída do caso de uso e na resposta | `UUID`, serializado como texto pelo conversor JSON já em uso | `String` montada no caso de uso | É o tipo do identificador no domínio e nos demais DTOs; evita conversão manual |
| Posição do campo nos `record` | Primeiro componente (`id`, depois os atuais) | Último componente | Segue a ordem dos outros DTOs de resposta; em JSON a ordem não é contrato |
| Origem do valor | O `Usuario` já carregado no login | Nova consulta | RNF-03: nenhuma consulta adicional |

## Padrões de projeto aplicados

| Padrão | Onde | Problema que resolve | Custo aceito |
|---|---|---|---|
| Nenhum | — | — | — |

Nenhum padrão foi considerado: a mudança acrescenta um campo a dois `record` existentes.

## Arquivos a criar ou alterar

| Camada | Arquivo | Ação | Teste espelhado |
|---|---|---|---|
| core/application | `core/application/usecases/auth/LoginUseCase.java` | alterar: `Output` ganha `id` | `core/application/usecases/auth/LoginUseCaseTest.java` (alterar) |
| adapters/controllers | `adapter/in/web/dto/response/LoginResponse.java` | alterar: ganha `id` | coberto por `adapter/in/web/auth/AuthControllerTest.java` |
| adapters/controllers | `adapter/in/web/auth/AuthController.java` | alterar: repassa `id` | `adapter/in/web/auth/AuthControllerTest.java` (alterar) |
| documentação | `docs/casos-de-uso.md` | alterar: UC-01 cita os campos da resposta | — |

Rastreabilidade: RF-01 nos três arquivos de produção; RF-02 e RNF-01 em `LoginResponse` e
`AuthController`; RF-03 em `AuthController` (ramo de falha, inalterado); RNF-03 em
`LoginUseCase`; RNF-02 nos dois arquivos de teste.

## Contrato entre camadas

`AuthController` chama `LoginUseCase`, que devolve `Output` com `id`, `token`,
`refreshToken`, `nome` e `perfil`. O controller copia os cinco valores para
`LoginResponse`. Falha de credencial continua sendo `NaoAutorizadoException`, traduzida
pelo próprio controller em 401 com `ErrorResponse`, que não tem campo `id`.

## Dependências externas

| Dependência | Versão | Justificativa | Simulada nos testes por |
|---|---|---|---|
| Nenhuma nova | — | — | — |

## Impacto no contrato de operação

Nenhum. Vale o desvio já registrado na feature 001: sem alvos `it` e `bdd`; os cenários de
aceite são cobertos por testes unitários e de controller.

## Riscos

| Risco | Probabilidade | Mitigação |
|---|---|---|
| Cliente que rejeita campo desconhecido na resposta | baixa | O único cliente é o `rgm-frontend`, que ignora campos extras |
| Outro ponto do código constrói `LoginUseCase.Output` ou `LoginResponse` | baixa | Busca no código mostrou só o controller e os dois testes; o compilador acusa o restante |

## Conformidade com a constituição

| Princípio | Como este plano o respeita |
|---|---|
| Contrato de operação | Validação por `make validate`; nenhum alvo novo |
| Arquitetura limpa | O dado nasce no caso de uso e o controller só traduz; `core` não ganha import |
| Testes provam a entrega | Um teste novo por cenário, em AAA, `shouldXWhenY` e `verifyNoMoreInteractions`; testes existentes só recebem o argumento novo |
| Simplicidade defensável | Um campo em dois `record`; nenhum padrão |
| Autoria | Commits e PR só com o autor do `git config` |
| Idioma | Artefatos em português |
| Mapa de camadas (7) | Pacotes existentes |
| Linguagem ubíqua (8) | Campo `id`, como nos demais DTOs |
| Compatibilidade com produção (9) | Mudança aditiva; nenhum campo removido ou renomeado |
| Cobertura não regride (10) | Mínimo intocado; arquivos alterados com 95% ou mais |
| Padrão de teste no que for tocado (11) | Testes novos no padrão; os existentes mantêm o formato |
| Uma fonte de especificação (12) | Spec em `specs/002-...` |
