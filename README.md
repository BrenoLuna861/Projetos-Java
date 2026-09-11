# Projetos Java — Portfólio Backend

Três projetos de backend em **Java 21 + Spring Boot 3.3**, cada um construído em torno de um problema específico que aparece em sistema de verdade: regra de negócio que muda toda semana, consistência entre banco e mensageria, e consulta que só fica lenta quando a tabela cresce.

Cada pasta é um projeto independente, com seu próprio `pom.xml`, testes e README detalhado.

**Breno Luna** · Cursando Ciência da Computação (FACAPE — Petrolina/PE)
[LinkedIn](https://www.linkedin.com/in/br%C3%AA/) · brenoluna914@gmail.com

---

## Os projetos

### [`credito-score-api`](./credito-score-api)

API de análise de crédito com **regras de pontuação plugáveis**.

O problema: política de crédito muda o tempo todo, e o código costuma virar um `if` gigante que ninguém tem coragem de tocar.

A solução: **Strategy** (cada regra é uma classe isolada e testável), **Factory** (cada produto — pessoal, veículo, imobiliário — tem sua própria política) e **Observer** (auditoria e alerta de risco reagem à análise sem que o serviço saiba quem escuta). O resultado é um serviço de orquestração com menos de 80 linhas que **não contém nenhuma regra de crédito**.

Cada análise devolve o porquê da decisão, regra a regra — o que permite explicar uma negativa ao cliente e auditar o processo depois.

`Spring Boot` · `Spring Data JPA` · `Bean Validation` · `H2` · `JUnit 5` · `MockMvc`

---

### [`pedidos-event-driven`](./pedidos-event-driven)

Serviço de pedidos que publica eventos no Kafka usando **outbox transacional**.

O problema: banco e broker são dois sistemas diferentes, e não existe transação atômica entre eles. Publicar dentro da transação cria evento de pedido que não existe; publicar depois perde o evento quando o broker cai.

A solução: o evento é gravado **na mesma transação do pedido**, numa tabela de outbox. Um publisher agendado lê essa tabela e envia ao Kafka fora da transação, com contador de tentativas e teto. Se o rollback acontecer, o evento some junto; se o Kafka cair, o pedido é salvo e o evento é reenviado depois.

O preço é entrega *pelo menos uma vez* — por isso a chave da mensagem é o id do pedido e o consumidor é idempotente.

`Spring Kafka` · `Spring Data JPA` · `H2 / PostgreSQL` · `Docker Compose` · `JUnit 5` · `Mockito`

---

### [`consultas-performance-api`](./consultas-performance-api)

Comparação entre **paginação por OFFSET e por keyset** sobre 500 mil linhas no PostgreSQL.

O problema: `OFFSET 400000` não pula as linhas — o banco lê, ordena e descarta todas elas para devolver 20. O custo cresce com a profundidade da página, e o bug só aparece onde ninguém testa.

A solução: cursor com comparação de valor de linha — `WHERE (ocorrido_em, id) < (?, ?)` — apoiada num índice composto na mesma ordem do `ORDER BY`. O banco desce direto à posição e o custo fica constante.

Inclui seeder em lote, `ANALYZE` após a carga e um endpoint de benchmark que mede as duas estratégias na mesma base — os números saem da execução, não de uma tabela inventada no README.

`Spring JDBC` · `PostgreSQL 16` · `Docker Compose` · `JUnit 5`

---

## Rodando qualquer um deles

```bash
cd <pasta-do-projeto>
mvn test            # roda os testes
mvn spring-boot:run # sobe a aplicação
```

Os projetos que dependem de Kafka ou PostgreSQL trazem `docker-compose.yml`:

```bash
docker compose up -d
```

Cada README explica o problema, as decisões de projeto e o que eu faria diferente em produção.
