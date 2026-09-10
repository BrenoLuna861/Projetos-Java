# consultas-performance-api

Comparação prática entre **paginação por OFFSET e paginação por keyset** sobre uma tabela de 500 mil linhas no PostgreSQL, com os índices que sustentam cada estratégia e um endpoint que mede as duas na mesma base.

**Stack:** Java 21 · Spring Boot 3.3 · Spring JDBC · PostgreSQL 16 · Docker Compose · JUnit 5

---

## O problema

Paginação com `OFFSET` funciona bem nas primeiras páginas e engasga nas últimas:

```sql
SELECT * FROM evento_acesso
 ORDER BY ocorrido_em DESC, id DESC
 LIMIT 20 OFFSET 400000;
```

O banco **não pula** as 400 mil linhas: ele as lê, ordena e joga fora, para devolver 20. O custo cresce junto com a profundidade da página — e o bug só aparece onde ninguém testa, que é no fim da lista.

Há um segundo problema, mais silencioso: se uma linha nova entrar enquanto o usuário navega, as páginas **deslocam**, e ele vê o mesmo registro duas vezes ou pula um sem perceber.

## A solução: keyset

Em vez de "pule N linhas", diz-se "continue depois DESTE ponto":

```sql
SELECT * FROM evento_acesso
 WHERE (ocorrido_em, id) < ('2026-03-14 10:30:45', 987654)
 ORDER BY ocorrido_em DESC, id DESC
 LIMIT 20;
```

O `(ocorrido_em, id) < (?, ?)` é uma **comparação de valor de linha** do PostgreSQL, e é ela que permite usar o índice composto inteiro: o banco desce a árvore direto até a posição e lê 20 linhas. O custo é constante, esteja o usuário na página 1 ou na 20.000.

O `id` no cursor não é enfeite — é o **desempate**. Sem ele, registros com o mesmo `ocorrido_em` fariam a paginação pular ou repetir linhas.

## O índice que sustenta tudo

```sql
CREATE INDEX idx_evento_ordenacao
    ON evento_acesso (ocorrido_em DESC, id DESC);
```

A ordem das colunas é exatamente a do `ORDER BY`. Índice com ordem diferente da consulta obriga o banco a ordenar de novo — e aí a paginação keyset perde a vantagem.

## Como rodar

```bash
docker compose up -d      # PostgreSQL 16
mvn spring-boot:run       # porta 8082
```

Na primeira execução o `DataSeeder` gera **500 mil eventos** em lotes de 5 mil e roda `ANALYZE` (sem estatísticas atualizadas, o planejador escolhe mal). Leva alguns minutos; execuções seguintes detectam a base populada e pulam.

Para testar mais rápido, reduza em `application.yml`:

```yaml
app:
  seed:
    quantidade: 50000
```

## Medindo

```bash
# uma profundidade específica
curl "http://localhost:8082/api/v1/benchmark?profundidade=400000&tamanho=20&repeticoes=5"

# a curva inteira: 0, 1k, 10k, 100k, 250k, 400k
curl "http://localhost:8082/api/v1/benchmark/curva"
```

A resposta traz a média de cada estratégia, quantas vezes o keyset foi mais rápido e as medições individuais.

**Os números dependem da sua máquina, do volume e do cache do banco — por isso este README não traz uma tabela de resultados fixa.** Rode o endpoint e registre o que a sua execução mostrou. O método já está no código: a primeira execução é descartada (aquecimento de cache) e a média sai das repetições seguintes.

Para ver o plano de execução por trás da diferença:

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM evento_acesso ORDER BY ocorrido_em DESC, id DESC LIMIT 20 OFFSET 400000;
```

Compare o `rows removed` e o número de buffers lidos com os da versão keyset.

## Usando a API

```bash
# offset (para comparação)
curl "http://localhost:8082/api/v1/eventos/offset?pagina=0&tamanho=20"

# keyset — a primeira chamada vai sem cursor
curl "http://localhost:8082/api/v1/eventos/keyset?tamanho=20"

# as seguintes usam o proximoCursor devolvido pela anterior
curl "http://localhost:8082/api/v1/eventos/keyset?tamanho=20&cursor=MjAyNi0wMy0xNFQxMDozMDo0NXw5ODc2NTQ"
```

## Decisões de projeto

- **SQL nativo, não ORM.** O ponto do projeto é o plano de execução, e ORM esconde exatamente isso.
- **Cursor opaco em Base64 URL-safe**, não o timestamp cru na query string. O cliente não deve depender do formato interno, e assim o cursor pode mudar sem quebrar quem consome.
- **Cursor inválido devolve erro claro**, não uma consulta silenciosamente errada.
- **Teto de 200 itens por página**: parâmetro de tamanho vindo do cliente é vetor de abuso.
- **`ANALYZE` após o seed**, sem o qual o PostgreSQL planeja com estatísticas de tabela vazia e o benchmark mede a coisa errada.

## Limitação honesta do keyset

Keyset **não permite pular para a página 500** — só avançar e voltar. Para navegação sequencial (scroll infinito, listagens, exportação, APIs públicas) isso é irrelevante e a troca compensa. Se o produto exige realmente saltar para uma página arbitrária no meio de milhões de linhas, o caminho é outro: filtro melhor, busca por período, ou índice específico para o recorte.

## Testes

```bash
mvn test
```

Cobrem a codificação e decodificação do cursor, incluindo os casos de valor inválido e de segurança para URL. Os testes de consulta exigem o PostgreSQL do compose no ar.
