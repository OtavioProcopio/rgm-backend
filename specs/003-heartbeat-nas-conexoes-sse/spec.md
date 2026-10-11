# Especificação — Heartbeat nas conexões SSE

> Descreve **o quê** e **por quê**. Não descreve como implementar: sem nome de biblioteca,
> sem esquema de banco, sem assinatura de função.

Origem: OtavioProcopio/rgm-backend#88 (prioridade média). Relacionada:
OtavioProcopio/rgm-frontend#113.

> **Registro de processo:** esta especificação foi escrita em 2026-10-05 **depois** da
> implementação, para regularizar a feature no padrão de specs. A ordem correta era
> especificar antes. Os pontos que a issue não determinava estão em **Ambiguidades**,
> marcados para confirmação.

## Problema

O quadro de solicitações recebe atualizações em tempo real por uma conexão que o servidor
mantém aberta com cada navegador. O servidor só escreve nessa conexão quando acontece um
evento. Em períodos sem evento, a conexão fica sem tráfego, e os proxies entre o navegador
e a API (o do frontend, com limite de leitura de 60 s, e o do painel de hospedagem) a
encerram.

O navegador então reconecta em ciclo, e todo evento publicado no intervalo entre a queda e
a reconexão se perde. Para o usuário, o quadro "para de atualizar sozinho" e só volta ao
normal quando ele recarrega a página.

## Objetivo

Uma conexão de tempo real sem eventos continua aberta, porque o servidor envia um sinal
periódico que mantém tráfego nela. Conexões que já caíram são descartadas pelo servidor.

## Fora de escopo

- Configuração do proxy do frontend para não reter os eventos (rgm-frontend#113).
- Novos tipos de evento (#87).
- Reenvio de eventos perdidos durante uma reconexão.
- Autenticação da conexão por token curto (#93).
- Tornar o intervalo do sinal configurável por ambiente.

## Personas e cenários de uso

- **Gestor com o quadro aberto o dia todo:** espera ver solicitações novas e mudanças de
  status sem recarregar a página, mesmo depois de minutos sem movimento.
- **Operador no detalhe de uma solicitação:** espera que a tela reflita o que os outros
  fazem enquanto ele está nela.

## Requisitos funcionais

| ID | Requisito | Prioridade |
|---|---|---|
| RF-01 | O sistema deve enviar, a cada conexão de tempo real aberta, um sinal periódico que não é um evento de solicitação e que o cliente ignora. | obrigatório |
| RF-02 | O sistema deve descartar a conexão de tempo real em que o envio do sinal falhar, sem afetar as demais conexões. | obrigatório |
| RF-03 | O sistema deve continuar entregando os eventos de solicitação nas conexões abertas, sem mudança de formato. | obrigatório |
| RF-04 | O sistema deve manter aberta por mais tempo a conexão que recebe o sinal periódico, encerrando-a só ao atingir o tempo limite da conexão. | obrigatório |

## Requisitos não funcionais

| ID | Requisito | Critério mensurável |
|---|---|---|
| RNF-01 | Intervalo do sinal periódico | 25 segundos entre envios, abaixo dos 60 segundos de limite de leitura do proxy |
| RNF-02 | Tempo limite da conexão de tempo real | 30 minutos (era 5 minutos na v1.5.0) |
| RNF-03 | Compatibilidade do contrato | 0 tipos de evento removidos ou renomeados em relação à v1.5.0 |
| RNF-04 | Cobertura de testes dos arquivos alterados | no mínimo 95% de linha em cada arquivo modificado que entra na medição |

## Critérios de aceite

```gherkin
# language: pt
Funcionalidade: Heartbeat nas conexões SSE

  Cenário: Toda conexão aberta recebe o sinal periódico (RF-01)
    Dado que existem 2 conexões de tempo real abertas
    Quando o sinal periódico é disparado
    Então cada uma das 2 conexões recebe 1 comentário "ping"
    Mas nenhuma conexão recebe evento de solicitação

  Cenário: Conexão que falha no envio é descartada (RF-02)
    Dado que existe 1 conexão de tempo real cujo envio falha por conexão fechada
    E que o sinal periódico já foi disparado 1 vez
    Quando o sinal periódico é disparado de novo
    Então a conexão não recebe nova tentativa de envio

  Cenário: Conexão já encerrada é descartada (RF-02)
    Dado que existe 1 conexão de tempo real já encerrada pelo servidor
    E que o sinal periódico já foi disparado 1 vez
    Quando o sinal periódico é disparado de novo
    Então a conexão não recebe nova tentativa de envio

  Cenário: Sinal periódico sem conexões abertas (RF-01)
    Dado que não existe conexão de tempo real aberta
    Quando o sinal periódico é disparado
    Então nenhum erro ocorre

  Cenário: Eventos de solicitação continuam sendo entregues (RF-03)
    Dado que existe 1 conexão de tempo real aberta
    Quando um evento de solicitação é publicado
    Então a conexão recebe 1 evento "solicitacao"
```

RF-04 e RNF-02 não têm cenário automatizado: o controlador que abre a conexão está fora da
medição de cobertura desde antes desta feature (ver `.specify/memory/as-is.md`, seção 5).

## Ambiguidades

Nenhuma em aberto. Resolvidas em `/bu:clarify` em 2026-10-05:

| Pergunta | Decisão |
|---|---|
| Tempo limite da conexão | 30 minutos, o valor sugerido na issue (RNF-02). Decisão técnica, sem pergunta ao usuário. |
| Intervalo do sinal configurável por ambiente? | Não: 25 segundos fixos, como a issue descreve (RNF-01). Decisão técnica, sem pergunta ao usuário. |

## Métricas de sucesso

- Em produção, depois de publicada também a rgm-frontend#113, uma aba parada por 10 minutos
  no quadro mantém a mesma conexão, sem reconexões no log do proxy.
- Nenhum relato de "quadro só atualiza ao recarregar".
