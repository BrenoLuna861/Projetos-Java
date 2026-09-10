# pedidos-event-driven

Serviço de pedidos que publica eventos no Kafka usando **outbox transacional**.

O projeto existe para resolver um problema específico e muito comum: garantir que **nunca exista um pedido salvo cujo evento se perdeu, nem um evento publicado de um pedido que não foi salvo**.

**Stack:** Java 21 · Spring Boot 3.3 · Spring Kafka · Spring Data JPA · H2 / PostgreSQL · Docker Compose · JUnit 5 + Mockito

---

## O problema

A forma ingênua de publicar um evento é esta:

```java
@Transactional
public void criar(Pedido pedido) {
    pedidoRepository.save(pedido);
    kafkaTemplate.send("pedidos.eventos", evento);  // ERRADO
}
```

Há duas falhas, e as duas acontecem em produção:

1. O `send` sai, e o commit da transação falha depois → existe um evento de um pedido que **não existe** no banco.
2. O commit funciona, mas o Kafka está fora do ar → o pedido existe e **ninguém foi avisado**. O estoque não reserva, o financeiro não cobra.

Banco e broker são dois sistemas diferentes. Não existe transação atômica entre eles.

## A solução: outbox

O evento é gravado **numa tabela do próprio banco, na mesma transação do pedido**:

```
┌─────────────────────────────────────────┐
│  TRANSAÇÃO ÚNICA                        │
│                                         │
│   INSERT INTO pedido       ──┐          │
│   INSERT INTO outbox_event ──┴─ commit  │
└─────────────────────────────────────────┘
                 │
                 │  (fora da transação, a cada 2s)
                 ▼
         ┌────────────────┐
         │ OutboxPublisher│──── publica ────▶  Kafka
         └────────────────┘
                 │
                 └── marca PUBLICADO, ou conta a tentativa e tenta de novo
```

Se a transação der rollback, o evento some junto com o pedido. Se o Kafka estiver fora, o pedido já está salvo e o evento fica `PENDENTE` até a próxima rodada. **Nenhum dos dois cenários de falha acima é possível.**

O preço dessa garantia é a entrega **pelo menos uma vez**: o mesmo evento pode ser publicado duas vezes se a publicação der certo e a marcação falhar. Por isso a chave da mensagem é o id do pedido e o consumidor é idempotente — ver `PedidoEventConsumer`.

## Como rodar

```bash
docker compose up -d      # sobe Kafka (KRaft) e PostgreSQL
mvn spring-boot:run       # a aplicação sobe na porta 8081
```

Por padrão a aplicação usa H2 em memória; o PostgreSQL do compose está pronto para quando quiser trocar a URL do datasource.

## Exemplo de uso

```bash
# cria o pedido — o evento nasce PENDENTE na mesma transação
curl -X POST http://localhost:8081/api/v1/pedidos \
  -H "Content-Type: application/json" \
  -d '{
    "cliente": "Maria Souza",
    "itens": [
      { "produto": "Teclado", "quantidade": 2, "precoUnitario": 150.00 },
      { "produto": "Monitor", "quantidade": 1, "precoUnitario": 900.00 }
    ]
  }'

# acompanha a fila de eventos
curl http://localhost:8081/api/v1/pedidos/outbox/status
# {"pendentes":0,"publicados":1,"falhas":0}
```

### Testando o cenário de falha

O jeito de ver o padrão funcionando é **derrubar o broker**:

```bash
docker compose stop kafka
curl -X POST http://localhost:8081/api/v1/pedidos -H "Content-Type: application/json" -d '{...}'
curl http://localhost:8081/api/v1/pedidos/outbox/status   # pendentes: 1 — o pedido foi salvo mesmo assim

docker compose start kafka
# aguarde alguns segundos
curl http://localhost:8081/api/v1/pedidos/outbox/status   # publicados: 1 — reenviado sozinho
```

## Decisões de projeto

- **Índice composto `(status, criado_em)`** na outbox: a consulta do publisher filtra por status e ordena por data. Sem esse índice, a varredura cresce junto com o histórico de eventos já publicados.
- **Lote limitado (`Limit.of(50)`)**: nunca carregar a outbox inteira em memória.
- **Contador de tentativas com teto**: depois de 5 falhas o evento vai para `FALHA` e para de ser tentado, em vez de entupir o lote para sempre. Em produção isso alimentaria uma DLQ e um alerta.
- **`acks=all` no producer**: só considera publicado quando as réplicas confirmam.
- **A serialização do evento acontece dentro da transação**: se falhar, o pedido não é criado. É o comportamento correto — melhor não ter o pedido do que tê-lo sem o evento.
- **Eventos publicados não são apagados**: viram trilha de auditoria. Uma rotina de expurgo por idade resolveria o crescimento.

## Testes

```bash
mvn test
```

Os testes **não precisam de Kafka rodando** — o `KafkaTemplate` é mockado. Cobrem: gravação do evento na mesma transação do pedido, publicação bem-sucedida, broker indisponível (evento continua `PENDENTE` e conta a tentativa) e o limite de tentativas levando a `FALHA`.

## O que faria diferente em produção

- Troca do polling por **CDC (Debezium)** lendo o WAL do PostgreSQL — elimina a latência do intervalo e a carga do `SELECT` periódico.
- Várias instâncias exigem `SELECT ... FOR UPDATE SKIP LOCKED` no lote, para dois publishers não pegarem o mesmo evento.
- Tabela de mensagens processadas no consumidor, no lugar do `Set` em memória.
