# Especificação — Instrução da triagem como campo da solicitação

> Descreve **o quê** e **por quê**. Não descreve como implementar: sem nome de biblioteca,
> sem esquema de banco, sem assinatura de função.

Origem: decisão do usuário em 2026-11-09, depois da spec 014 (PR #139): a instrução de
triagem é uma **decisão** e precisa viver na solicitação, não no histórico. No mesmo dia o
usuário decidiu que o gestor pode **corrigir** a instrução enquanto a solicitação estiver em
andamento, sem apagar o que existia (revisão desta spec antes de qualquer código). Consumidor:
`rgm-frontend`, spec 018. Base: `develop` com a spec 014 mergeada.

> **Registro de processo (honestidade):** a Fase 1 (refatoração do agregado) foi começada no
> código antes de esta spec refletir a decisão de editar a instrução. Nada foi commitado: o
> trabalho parcial ficou guardado em `git stash` e só será retomado depois da aprovação desta
> revisão.

## Problema

1. **A instrução está no lugar errado.** A spec 014 a grava como atividade de comentário. Ela
   se mistura à conversa, só se distingue pela posição, e para exibi-la em destaque o cliente
   teria de adivinhar qual comentário é a instrução.
2. **A triagem pode ser refeita fora de `A_FAZER`.** Hoje `/triar` aceita uma solicitação
   `EM_VALIDACAO` (a transição para `EM_ANDAMENTO` vale para os dois), o que reatribui
   responsáveis e prioridade. Com a instrução como campo, isso a sobrescreveria.
3. **O agregado `Solicitacao` é caro de evoluir.** Cada transição recopia os 16 campos num
   construtor público (9 cópias no próprio agregado, mais o mapper) e ~50 testes repetem a
   lista. Acrescentar um campo exige editar 62 pontos em 24 arquivos, e trocar dois campos do
   mesmo tipo compila sem erro.

4. **Uma instrução errada não tem como ser corrigida.** Se o gestor se enganou ao triar, hoje
   só resta comentar por cima. Corrigir não pode apagar o histórico: quem executa precisa saber
   que a instrução mudou e o que dizia antes.

**Custo de não resolver:** a instrução continua sem destaque nem proteção, a triagem pode
apagá-la, a correção vira comentário solto, e cada campo novo custa dezenas de edições mecânicas.

## Objetivo

- A solicitação tem o campo `instrucao`, gravado na triagem.
- Só se tria uma solicitação em `A_FAZER`.
- Gestor e administrador corrigem a instrução enquanto a solicitação está em andamento, e cada
  correção fica registrada no histórico com o texto anterior.
- Acrescentar um campo ao agregado passa a tocar um lugar só no código de produção e um nos testes.

## Fora de escopo

- Editar a instrução fora de `EM_ANDAMENTO` (em validação, concluída ou cancelada).
- Editar a instrução pelo responsável (operador).
- Desfazer uma correção; a anterior fica no histórico e pode ser digitada de novo.
- Preencher a instrução das triagens anteriores (não há dado confiável; ver Esclarecimentos).
- Remover as atividades de comentário já gravadas pela spec 014.
- Mudar o front: o consumo é da spec 018 do `rgm-frontend`.
- Endpoint para editar ou excluir comentário (não existe e não passa a existir).

## Personas e cenários de uso

- **Gestor e administrador:** triam e dizem o que fazer; corrigem a instrução enquanto a solicitação está em andamento.
- **Responsável (operador):** abre a solicitação e lê a instrução em destaque; vê no histórico quando ela mudou e o que dizia antes.
- **Quem mantém o código:** acrescenta campos ao agregado sem editar dezenas de arquivos.

## Requisitos funcionais

| ID | Requisito | Prioridade |
|---|---|---|
| RF-01 | A solicitação tem o campo `instrucao` (texto, até 2000 caracteres), nulo em `A_FAZER` e nas triadas antes deste campo | obrigatório |
| RF-02 | A triagem exige `instrucao`: ausente, vazia, só com espaços ou acima de 2000 caracteres é recusada com 400 e nada é gravado | obrigatório |
| RF-03 | A triagem grava `instrucao`, sem os espaços das pontas, na própria solicitação, na mesma operação que muda o status | obrigatório |
| RF-04 | A triagem só é aceita para solicitação em `A_FAZER`; em qualquer outro status a resposta é 409 e a solicitação não muda | obrigatório |
| RF-05 | A triagem deixa de criar atividade de comentário; o histórico mantém a atribuição e a mudança de status | obrigatório |
| RF-06 | O campo do corpo de `PATCH /api/solicitacoes/{id}/triar` passa a se chamar `instrucao`, no lugar de `descricao` (spec 014) | obrigatório |
| RF-07 | `instrucao` sai em toda resposta que hoje traz `descricao` da solicitação (detalhe, listagem e eventos), nula quando não houver | obrigatório |
| RF-08 | A instrução só muda pela edição de instrução (RF-10): nenhuma outra ação existente (editar, devolver, enviar para validação, encerrar, cancelar, trocar responsáveis) a altera | obrigatório |
| RF-09 | A ficha do modelo em PDF mostra a instrução de cada solicitação que a tem, logo depois da descrição | obrigatório |
| RF-10 | Gestor e administrador editam a instrução de uma solicitação `EM_ANDAMENTO` por `PATCH /api/solicitacoes/{id}/instrucao` com `{ "instrucao": "…" }`; valem as mesmas regras de RF-02 e RF-03 (obrigatória, até 2000 caracteres, sem espaços nas pontas) | obrigatório |
| RF-11 | Em qualquer outro status a edição é recusada com 409 e a instrução não muda: `A_FAZER` (a instrução nasce na triagem), `EM_VALIDACAO`, `CONCLUIDA` e `CANCELADA`. Se a solicitação é devolvida e volta a `EM_ANDAMENTO`, pode ser editada de novo | obrigatório |
| RF-12 | Operador (inclusive o responsável) e perfil externo não editam a instrução: 403 | obrigatório |
| RF-13 | Cada edição que muda o texto registra no histórico uma atividade "instrução alterada", com autor, data e o texto anterior; não cria comentário comum. Texto igual ao atual (depois de aparado) não muda nada, não registra atividade e responde 200 | obrigatório |
| RF-14 | `acoesPermitidas` ganha `EDITAR_INSTRUCAO`, presente só para gestor e administrador com a solicitação `EM_ANDAMENTO` | obrigatório |
| RF-15 | A edição atualiza `atualizadaEm` e usa o controle de versão da solicitação: duas edições simultâneas não se sobrepõem (a segunda recebe 409); a resposta da edição e o evento "instrucao_editada" trazem a solicitação completa; um aviso de atividade ("solicitacao_atividade", como o do comentário) faz o histórico dos outros usuários atualizar | obrigatório |

## Requisitos não funcionais

| ID | Requisito | Critério mensurável |
|---|---|---|
| RNF-01 | Manutenibilidade do agregado | Acrescentar um campo à `Solicitacao` altera o `Builder`, o `toBuilder()` e o construtor no agregado e 1 ponto no mapper; `new Solicitacao(` em código de produção aparece em 1 ponto só (`Builder.build()`) |
| RNF-02 | Manutenibilidade dos testes | Os testes montam solicitações por `Solicitacao.builder()` (campos nomeados, só os que importam) ou pelo `SolicitacaoTestBuilder` (padrões válidos por status); `new Solicitacao(` não aparece em teste algum |
| RNF-03 | Refatoração sem mudança de comportamento | A suíte existente passa sem alterar nenhuma asserção, antes de qualquer campo novo |
| RNF-04 | Cobertura dos arquivos tocados | ≥ 95% de linhas por arquivo (Princípio 10); nenhum teste escrito só para cobrir linha |
| RNF-05 | Esquema | 1 migração nova, aditiva, coluna anulável, sem reescrever linhas existentes; o novo tipo de atividade não exige migração (a coluna `tipo` é texto livre) |
| RNF-06 | Verificação | `make validate` com 0 falhas e lint limpo |

## Ordem de publicação (Princípio 9)

Decisão do usuário (2026-11-09): o backend é feito por completo primeiro e o `rgm-frontend`
(spec 018) em seguida; o intervalo em que o front atual convive com o backend novo é aceito, sem
comportamento de reserva. Mudanças aditivas (campo `instrucao`, rota `/instrucao`, evento,
ação `EDITAR_INSTRUCAO`, tipo de atividade `INSTRUCAO_ALTERADA`) não quebram o contrato. O corpo
de `/triar` muda de `descricao` para `instrucao`: é a única quebra, declarada aqui, e o campo
`descricao` existe só no `develop` (spec 014), nunca foi publicado em produção.

## Critérios de aceite

```gherkin
# language: pt
Funcionalidade: Instrução como campo da solicitação

  Cenário: Triar grava a instrução na solicitação
    Dado uma solicitação em A_FAZER e um gestor
    Quando o gestor tria com a instrução "  Trocar o gabarito  "
    Então a solicitação fica EM_ANDAMENTO
    E o detalhe traz instrucao "Trocar o gabarito"
    E o histórico traz a atribuição e a mudança de status
    E o histórico não traz comentário da triagem

  Cenário: Sem instrução
    Dado uma solicitação em A_FAZER
    Quando o gestor tria sem instrucao, com ela vazia ou só com espaços
    Então a resposta é 400
    E a solicitação continua A_FAZER, sem instrucao

  Cenário: Instrução acima do limite
    Quando o gestor tria com 2001 caracteres
    Então a resposta é 400
    Mas com 2000 caracteres a triagem é feita

  Cenário: Triar de novo não sobrescreve
    Dado uma solicitação EM_VALIDACAO que já tem instrução
    Quando o gestor tenta triar de novo com outra instrução
    Então a resposta é 409
    E a instrução e os responsáveis continuam os mesmos

  Cenário: Triar solicitação em andamento, concluída ou cancelada
    Dado uma solicitação EM_ANDAMENTO, CONCLUIDA ou CANCELADA
    Quando o gestor tenta triar
    Então a resposta é 409

  Cenário: A instrução sobrevive às outras ações
    Dado uma solicitação triada com instrução
    Quando ela é editada, devolvida, enviada para validação, encerrada
    Então a instrução continua a mesma

  Cenário: Gestor corrige a instrução
    Dado uma solicitação EM_ANDAMENTO com a instrução "Trocar o gabarito"
    Quando o gestor edita a instrução para "  Trocar o gabarito e o pino  "
    Então a instrução passa a "Trocar o gabarito e o pino"
    E o histórico traz "instrução alterada" pelo gestor com o texto anterior "Trocar o gabarito"
    E o histórico não traz comentário novo
    E o evento "instrucao_editada" traz a solicitação completa
    E o aviso de atividade "instrucao_editada" é publicado para o histórico

  Cenário: Editar com o mesmo texto
    Dado uma solicitação EM_ANDAMENTO com a instrução "Trocar o gabarito"
    Quando o gestor edita para "  Trocar o gabarito "
    Então a resposta é 200
    E nada muda e o histórico não ganha atividade

  Cenário: Editar sem texto ou longo demais
    Dado uma solicitação EM_ANDAMENTO
    Quando o gestor edita com instrução ausente, vazia, só com espaços ou com 2001 caracteres
    Então a resposta é 400
    E a instrução continua a mesma

  Cenário: Editar fora de EM_ANDAMENTO
    Dado uma solicitação A_FAZER, EM_VALIDACAO, CONCLUIDA ou CANCELADA
    Quando o gestor tenta editar a instrução
    Então a resposta é 409
    E a instrução continua a mesma

  Cenário: Editar depois de devolver
    Dado uma solicitação EM_VALIDACAO devolvida para EM_ANDAMENTO
    Quando o gestor edita a instrução
    Então a edição é feita

  Cenário: Operador não edita
    Dado o operador responsável pela solicitação EM_ANDAMENTO
    Quando ele tenta editar a instrução
    Então a resposta é 403

  Cenário: Ação oferecida só a quem pode
    Dado uma solicitação EM_ANDAMENTO
    Quando se lê o detalhe como gestor, administrador, operador responsável e operador sem relação
    Então EDITAR_INSTRUCAO aparece em acoesPermitidas só para gestor e administrador

  Cenário: Duas edições ao mesmo tempo
    Dado dois gestores com o mesmo detalhe aberto
    Quando os dois enviam uma edição
    Então a primeira é feita e a segunda recebe 409

  Cenário: Solicitação triada antes do campo
    Dado uma solicitação EM_ANDAMENTO triada antes desta spec
    Quando se lê o detalhe
    Então instrucao é nula

  Cenário: Instrução em listagem e evento
    Dado uma solicitação triada com instrução
    Quando se lê a listagem e o evento "triada"
    Então ambos trazem a instrução

  Cenário: Regras antigas da triagem continuam valendo
    Dado um operador, um responsável inativo, um perfil não atribuível ou nenhum responsável
    Quando a triagem é pedida com instrução válida
    Então ela é recusada como antes

  Cenário: Ficha do modelo em PDF mostra a instrução
    Dado um modelo com uma solicitação triada com instrução e outra sem
    Quando se exporta a ficha do modelo
    Então o PDF mostra a instrução da primeira
    E não mostra linha de instrução na segunda

  Cenário: Corpo antigo da triagem
    Quando um cliente envia "descricao" no lugar de "instrucao"
    Então a resposta é 400

  Cenário: Refatoração não muda o comportamento
    Dado a suíte de testes anterior à refatoração
    Quando o agregado passa a copiar por um mecanismo único
    Então a suíte passa sem alterar nenhuma asserção
```

## Ambiguidades

- Nenhuma marca aberta.

## Esclarecimentos

| Pergunta | Resposta | Data |
|---|---|---|
| A instrução vira campo da solicitação? | Sim: é decisão, não comentário | 2026-11-09 |
| Pode editar a instrução depois? | Sim, gestor e administrador, enquanto a solicitação está em andamento; trava quando vai para validação e volta a destravar se for devolvida | 2026-11-09 |
| A correção apaga o texto antigo? | Não: a atividade "instrução alterada" guarda o texto anterior, com autor e data | 2026-11-09 |
| Quem corrige? | Gestor e administrador (mesma regra de triar e de trocar responsáveis); o responsável não | 2026-11-09 |
| Preenche as triagens antigas? | Não. O dado só existe como comentário solto, sem marca que o distinga de uma conversa; copiar seria adivinhar | 2026-11-09 |
| `descricao` do corpo de `/triar` | Renomeada para `instrucao`: `descricao` já é o texto que o operador escreve ao abrir | 2026-11-09 |
| Instrução na listagem? | Sim, como `descricao`: o peso é o mesmo (limite de 2000) e o detalhe atualiza por evento | 2026-11-09 |

## Métricas de sucesso

- 0 solicitações triadas, depois da publicação, sem `instrucao`.
- 0 triagens aceitas fora de `A_FAZER`.
- 0 edições de instrução sem atividade correspondente no histórico (exceto texto igual).
- Um campo novo no agregado custa ≤ 3 arquivos de produção e 1 de teste.
