package com.brenoluna.performance.repository;

import com.brenoluna.performance.domain.Cursor;
import com.brenoluna.performance.domain.EventoAcesso;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

/**
 * As duas estrategias de paginacao, lado a lado, em SQL nativo -
 * proposital: o ponto do projeto e o plano de execucao, e ORM esconde isso.
 */
@Repository
public class EventoRepository {

    private static final RowMapper<EventoAcesso> MAPPER = (rs, i) -> new EventoAcesso(
            rs.getLong("id"),
            rs.getLong("usuario_id"),
            rs.getString("acao"),
            rs.getString("ip"),
            rs.getTimestamp("ocorrido_em").toLocalDateTime());

    private static final String COLUNAS = "id, usuario_id, acao, ip, ocorrido_em";

    private final JdbcTemplate jdbc;

    public EventoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * OFFSET: o banco precisa varrer e DESCARTAR todas as linhas anteriores.
     * Custo cresce linearmente com a profundidade da pagina - o problema
     * so aparece nas paginas do fim, que e onde ninguem testa.
     */
    public List<EventoAcesso> porOffset(int tamanho, int offset) {
        String sql = """
                SELECT %s
                  FROM evento_acesso
                 ORDER BY ocorrido_em DESC, id DESC
                 LIMIT ? OFFSET ?
                """.formatted(COLUNAS);
        return jdbc.query(sql, MAPPER, tamanho, offset);
    }

    /**
     * KEYSET: o banco salta direto para a posicao no indice usando comparacao
     * de tupla. Custo constante, independente da profundidade.
     *
     * A comparacao (ocorrido_em, id) < (?, ?) e uma comparacao de VALOR DE LINHA,
     * suportada pelo PostgreSQL, e e o que permite usar o indice composto inteiro.
     */
    public List<EventoAcesso> porKeyset(int tamanho, Cursor cursor) {
        if (cursor == null) {
            String sql = """
                    SELECT %s
                      FROM evento_acesso
                     ORDER BY ocorrido_em DESC, id DESC
                     LIMIT ?
                    """.formatted(COLUNAS);
            return jdbc.query(sql, MAPPER, tamanho);
        }

        String sql = """
                SELECT %s
                  FROM evento_acesso
                 WHERE (ocorrido_em, id) < (?, ?)
                 ORDER BY ocorrido_em DESC, id DESC
                 LIMIT ?
                """.formatted(COLUNAS);

        return jdbc.query(sql, MAPPER,
                Timestamp.valueOf(cursor.ocorridoEm()), cursor.id(), tamanho);
    }

    public long total() {
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM evento_acesso", Long.class);
        return total == null ? 0L : total;
    }

    /** Devolve o plano de execucao real - usado pelo endpoint de benchmark. */
    public List<String> explicar(String sql, Object... parametros) {
        return jdbc.query("EXPLAIN (ANALYZE, BUFFERS) " + sql,
                (rs, i) -> rs.getString(1), parametros);
    }
}
