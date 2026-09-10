package com.brenoluna.performance.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Popula a base na primeira execucao. Sem volume nao existe diferenca
 * mensuravel entre as duas paginacoes - o projeto inteiro depende disso.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String[] ACOES = {"LOGIN", "LOGOUT", "CONSULTA", "DOWNLOAD", "EXPORTACAO", "FALHA_LOGIN"};

    private final JdbcTemplate jdbc;

    @Value("${app.seed.quantidade:500000}")
    private int quantidade;

    @Value("${app.seed.lote:5000}")
    private int lote;

    public DataSeeder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        Long existentes = jdbc.queryForObject("SELECT COUNT(*) FROM evento_acesso", Long.class);
        if (existentes != null && existentes >= quantidade) {
            log.info("Base ja possui {} eventos - seed ignorado", existentes);
            return;
        }

        int faltando = quantidade - (existentes == null ? 0 : existentes.intValue());
        log.info("Gerando {} eventos de acesso (lotes de {})...", faltando, lote);

        long inicio = System.currentTimeMillis();
        LocalDateTime base = LocalDateTime.now().minusYears(2);
        ThreadLocalRandom random = ThreadLocalRandom.current();

        List<Object[]> buffer = new ArrayList<>(lote);

        for (int i = 0; i < faltando; i++) {
            buffer.add(new Object[]{
                    random.nextLong(1, 50_000),
                    ACOES[random.nextInt(ACOES.length)],
                    "%d.%d.%d.%d".formatted(random.nextInt(1, 255), random.nextInt(256),
                            random.nextInt(256), random.nextInt(1, 255)),
                    Timestamp.valueOf(base.plusMinutes(random.nextLong(0, 1_051_200)))
            });

            if (buffer.size() == lote) {
                gravar(buffer);
                buffer.clear();
            }
        }
        if (!buffer.isEmpty()) {
            gravar(buffer);
        }

        long segundos = (System.currentTimeMillis() - inicio) / 1000;
        log.info("Seed concluido em {}s", segundos);

        // Sem estatisticas atualizadas o planejador do PostgreSQL escolhe mal.
        jdbc.execute("ANALYZE evento_acesso");
    }

    private void gravar(List<Object[]> linhas) {
        jdbc.batchUpdate(
                "INSERT INTO evento_acesso (usuario_id, acao, ip, ocorrido_em) VALUES (?, ?, ?, ?)",
                linhas);
    }
}
