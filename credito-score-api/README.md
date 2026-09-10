# credito-score-api

API REST de análise de crédito construída para deixar explícito um ponto: **regra de negócio muda toda semana, e o código precisa aguentar isso sem virar um `if` gigante**.

Cada regra de pontuação é uma classe isolada; cada produto de crédito tem sua própria política; e o serviço que orquestra a análise não conhece nenhuma regra específica.

**Stack:** Java 21 · Spring Boot 3.3 · Spring Data JPA · Bean Validation · H2 · JUnit 5 + AssertJ

---

## Os três padrões e por que estão aqui

| Padrão | Onde | O problema que resolve |
|---|---|---|
| **Strategy** | `regras/RegraScore` e implementações | Adicionar uma regra nova não exige tocar no serviço de análise nem nas regras existentes. Cada regra é testável sozinha. |
| **Factory** | `factory/PoliticaScoreFactory` | Cada produto (pessoal, veículo, imobiliário) usa um conjunto diferente de regras. Trocar a política de um produto é mudança de uma linha, sem risco para os outros. |
| **Observer** | `observer/AnaliseObserver` e implementações | Auditoria e alerta de risco reagem à análise concluída sem que o serviço saiba quem está escutando. Adicionar um novo interessado não altera o fluxo principal. |

O ganho concreto: `AnaliseCreditoService` tem menos de 80 linhas e **não contém uma única regra de crédito**. Ele soma pontos e compara com o corte.

## Regras implementadas

| Regra | Peso | Critério |
|---|---|---|
| `COMPROMETIMENTO_RENDA` | +300 / +120 / −150 | Percentual da renda já comprometido (≤30% / ≤50% / acima) |
| `HISTORICO_PAGAMENTO` | +250 a −200 | 250 pontos base, −90 por atraso nos últimos 12 meses, com piso |
| `TEMPO_RELACIONAMENTO` | +150 / +80 / 0 | ≥60 meses / ≥12 meses / recente |
| `CAPACIDADE_PAGAMENTO` | +200 / +60 / −120 | Valor solicitado sobre a renda anual (≤1x / ≤3x / acima) |

Cortes de aprovação: 400 (pessoal), 500 (veículo), 600 (imobiliário). Até 100 pontos abaixo do corte a decisão é `APROVADO_COM_RESSALVA`.

## Como rodar

```bash
mvn spring-boot:run
```

A aplicação sobe em `http://localhost:8080` com banco H2 em memória (console em `/h2-console`).

## Exemplo de uso

```bash
curl -X POST http://localhost:8080/api/v1/analises \
  -H "Content-Type: application/json" \
  -d '{
    "documento": "12345678901",
    "produto": "CREDITO_PESSOAL",
    "rendaMensal": 6000,
    "dividasMensais": 1200,
    "valorSolicitado": 4000,
    "idade": 35,
    "atrasos12Meses": 0,
    "mesesRelacionamento": 48
  }'
```

Resposta:

```json
{
  "id": 1,
  "documento": "12345678901",
  "produto": "CREDITO_PESSOAL",
  "score": 630,
  "decisao": "APROVADO",
  "detalhes": [
    { "regra": "COMPROMETIMENTO_RENDA", "pontos": 300, "motivo": "Comprometimento de renda em 20.0%, dentro do limite saudavel de 30%" },
    { "regra": "HISTORICO_PAGAMENTO", "pontos": 250, "motivo": "Nenhum atraso registrado nos ultimos 12 meses" },
    { "regra": "TEMPO_RELACIONAMENTO", "pontos": 80, "motivo": "Cliente ha 48 meses" }
  ]
}
```

Cada análise devolve **o porquê da decisão**, regra a regra. Em crédito isso não é luxo: é o que permite explicar a negativa ao cliente e auditar o processo depois.

## Decisões de projeto

- **`SolicitacaoCredito` é um `record` imutável** — nenhuma regra consegue alterar a solicitação enquanto a avalia.
- **`BigDecimal` em todo valor monetário**, nunca `double`. Erro de arredondamento em crédito vira problema contábil.
- **Documento é mascarado no log de auditoria** (`***7890`) — dado pessoal completo não vai para arquivo de log.
- **Divisão por zero tratada explicitamente**: renda declarada igual a zero vira risco máximo em vez de exceção.
- **Falha de política é `422`, falha de validação é `400`** — o cliente da API precisa distinguir "mandei errado" de "não consigo processar isso".

## Testes

```bash
mvn test
```

Cobrem: cada regra isoladamente (incluindo os limites e o caso de renda zero), a decisão do serviço nos três produtos, e a API de ponta a ponta com MockMvc.

## O que faria diferente em produção

- Regras e cortes em banco ou arquivo de configuração, não em constante Java — a área de risco muda esses números sem deploy.
- Observers assíncronos (`@Async` ou evento de domínio), para que auditoria lenta não segure a resposta.
- Versionamento da política aplicada em cada análise, para conseguir reprocessar uma decisão antiga com as regras da época.
