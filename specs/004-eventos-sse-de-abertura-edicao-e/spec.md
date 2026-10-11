# Especificação — Eventos SSE de abertura, edição e responsáveis

> Descreve **o quê** e **por quê**. Não descreve como implementar: sem nome de biblioteca,
> sem esquema de banco, sem assinatura de função.

Origem: OtavioProcopio/rgm-backend#87 (prioridade média). Relacionada:
OtavioProcopio/rgm-frontend#113.

> **Registro de processo:** esta especificação foi escrita em 2026-10-05 **depois** da
> implementação, para regularizar a feature no padrão de specs. A ordem correta era
> especificar antes. Os pontos que a issue não determinava estão em **Ambiguidades**,
> marcados para confirmação.

## Problema

O sistema avisa os navegadores em tempo real quando uma solicitação é triada, enviada para
validação, devolvida, encerrada ou cancelada. Não avisa quando uma solicitação é aberta,
quando título, descrição ou tipo são editados, quando os responsáveis mudam, quando alguém
comenta nem quando alguém anexa uma evidência.

Por isso uma solicitação nova não aparece no quadro dos outros usuários até alguém
recarregar a página, e o mesmo vale para edições e trocas de responsável. O gestor pode
demorar a triar um chamado que já existe, e um operador pode não saber que virou
responsável.

## Objetivo

Abertura, edição e troca de responsáveis passam a gerar aviso em tempo real, no mesmo
formato dos avisos existentes. Comentário e evidência anexada passam a gerar um aviso de
atividade, para a linha do tempo do detalhe atualizar sozinha. Os tipos de aviso ficam
documentados para o frontend.

## Fora de escopo

- Aviso de evidência excluída.
- Levar o texto do comentário ou os dados da evidência dentro do aviso.
- Atualização do detalhe da solicitação e do proxy no frontend (rgm-frontend#113).
- Sinal periódico de conexão (#88, feature 003).
- Filtrar os avisos por usuário ou por permissão de visualização.
- Reenvio de avisos perdidos.

## Personas e cenários de uso

- **Gestor com o quadro aberto:** vê a solicitação nova entrar na coluna A_FAZER sem
  recarregar.
- **Operador com o quadro aberto:** vê a solicitação em que foi colocado como responsável.
- **Qualquer usuário no quadro:** vê título e descrição corrigidos por outra pessoa.

## Requisitos funcionais

| ID | Requisito | Prioridade |
|---|---|---|
| RF-01 | O sistema deve publicar um aviso em tempo real do tipo `aberta` quando uma solicitação é aberta com sucesso. | obrigatório |
| RF-02 | O sistema deve publicar um aviso do tipo `editada` quando uma solicitação é editada com sucesso. | obrigatório |
| RF-03 | O sistema deve publicar um aviso do tipo `responsaveis_alterados` quando os responsáveis de uma solicitação são alterados com sucesso. | obrigatório |
| RF-04 | Os avisos novos devem ter o mesmo formato dos existentes: evento `solicitacao`, com o tipo e os dados da solicitação resultante. | obrigatório |
| RF-05 | O sistema deve manter os avisos existentes (`triada`, `enviada_validacao`, `devolvida`, `encerrada`, `cancelada`) com os mesmos nomes. | obrigatório |
| RF-06 | A documentação do projeto deve listar todos os tipos de aviso e quando cada um é publicado. | obrigatório |
| RF-07 | O aviso do tipo `responsaveis_alterados` deve trazer a lista de responsáveis resultante da alteração, e a resposta do pedido de alteração também. | obrigatório |
| RF-08 | O sistema deve publicar um aviso de atividade do tipo `comentada`, com o identificador da solicitação, quando um comentário é registrado com sucesso. | obrigatório |
| RF-09 | O sistema deve publicar um aviso de atividade do tipo `evidencia_adicionada`, com o identificador da solicitação, quando uma evidência é anexada com sucesso. | obrigatório |
| RF-10 | Os avisos de atividade devem usar um nome de evento próprio (`solicitacao_atividade`), distinto do evento `solicitacao`, para que clientes existentes não recebam um formato que não conhecem. | obrigatório |
| RF-11 | O sistema não deve publicar aviso quando o comentário ou o anexo é recusado. | obrigatório |

## Requisitos não funcionais

| ID | Requisito | Critério mensurável |
|---|---|---|
| RNF-01 | Compatibilidade do contrato | 0 tipos de aviso removidos ou renomeados em relação à v1.5.0 |
| RNF-02 | Custo da publicação | 0 consultas adicionais ao banco por abertura, edição, troca de responsáveis, comentário ou anexo |
| RNF-03 | Quantidade de avisos | exatamente 1 aviso por operação bem-sucedida |
| RNF-04 | Cobertura de testes dos arquivos alterados | no mínimo 95% de linha em cada arquivo modificado |

## Critérios de aceite

```gherkin
# language: pt
Funcionalidade: Eventos SSE de abertura, edição e responsáveis

  Cenário: Abertura publica aviso (RF-01, RF-04)
    Quando um usuário abre uma solicitação com sucesso
    Então a resposta é 201
    E é publicado 1 evento "solicitacao" com tipo "aberta" e os dados da solicitação aberta

  Cenário: Edição publica aviso (RF-02, RF-04)
    Quando um usuário edita uma solicitação com sucesso
    Então a resposta é 200
    E é publicado 1 evento "solicitacao" com tipo "editada" e os dados da solicitação editada

  Cenário: Troca de responsáveis publica aviso (RF-03, RF-04)
    Quando um gestor altera os responsáveis de uma solicitação com sucesso
    Então a resposta é 200
    E é publicado 1 evento "solicitacao" com tipo "responsaveis_alterados" e os dados da solicitação

  Cenário: Aviso de responsáveis traz a lista resultante (RF-07)
    Quando um gestor define o usuário "bruno" como único responsável de uma solicitação
    Então a resposta é 200 e a lista de responsáveis da resposta contém só "bruno"
    E o aviso "responsaveis_alterados" traz a mesma lista de responsáveis

  Cenário: Comentário publica aviso de atividade (RF-08, RF-10)
    Quando um usuário registra um comentário em uma solicitação com sucesso
    Então a resposta é 201
    E é publicado 1 evento "solicitacao_atividade" com tipo "comentada" e o identificador da solicitação

  Cenário: Evidência anexada publica aviso de atividade (RF-09, RF-10)
    Quando um usuário anexa uma evidência a uma solicitação com sucesso
    Então a resposta é 201
    E é publicado 1 evento "solicitacao_atividade" com tipo "evidencia_adicionada" e o identificador da solicitação

  Cenário: Comentário recusado não publica aviso (RF-11)
    Quando o registro de um comentário é recusado por falta de permissão
    Então a resposta é 403
    Mas nenhum evento é publicado

  Cenário: Anexo recusado não publica aviso (RF-11)
    Quando o anexo de uma evidência é recusado por falta de permissão
    Então a resposta é 403
    Mas nenhum evento é publicado
```

RF-05 é verificado pelos testes existentes das cinco transições; RF-06 é verificado por
leitura da documentação. O código de resposta do comentário e do anexo é o que a API já
devolve hoje; os cenários acima não o alteram.

## Ambiguidades

Nenhuma em aberto. Resolvidas em `/bu:clarify` em 2026-10-05:

| Pergunta | Decisão |
|---|---|
| Comentário e evidência anexada geram aviso? | Sim, nesta branch (RF-08, RF-09). Respondido pelo usuário. |
| O aviso `responsaveis_alterados` leva a lista de responsáveis? | Sim, preenchida (RF-07). Respondido pelo usuário. |
| Onde documentar os tipos de aviso? | Em `docs/casos-de-uso.md`. Respondido pelo usuário. |
| Formato do aviso de comentário e de evidência | Evento próprio `solicitacao_atividade` com tipo e identificador da solicitação (RF-10). Decisão técnica do plano: evita consulta adicional e não muda o formato do evento `solicitacao`. O usuário não foi consultado sobre este detalhe. |

## Métricas de sucesso

- Em produção, depois de publicada também a rgm-frontend#113, uma solicitação aberta por um
  usuário aparece no quadro de outro em até 5 segundos, sem recarregar a página.
