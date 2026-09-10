package com.brenoluna.performance.domain;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Ponteiro para a ultima linha da pagina anterior.
 *
 * A paginacao keyset nao usa numero de pagina: ela usa a posicao real no
 * indice. O cursor carrega exatamente as colunas do ORDER BY
 * (ocorrido_em, id) - o "id" entra para desempatar registros com o mesmo
 * timestamp, sem o que a paginacao pularia ou repetiria linhas.
 */
public record Cursor(LocalDateTime ocorridoEm, long id) {

    private static final String SEPARADOR = "|";

    public String codificar() {
        String bruto = ocorridoEm.toString() + SEPARADOR + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(bruto.getBytes(StandardCharsets.UTF_8));
    }

    public static Cursor decodificar(String valor) {
        try {
            String bruto = new String(Base64.getUrlDecoder().decode(valor), StandardCharsets.UTF_8);
            String[] partes = bruto.split("\\" + SEPARADOR);
            if (partes.length != 2) {
                throw new IllegalArgumentException("Cursor mal formado");
            }
            return new Cursor(LocalDateTime.parse(partes[0]), Long.parseLong(partes[1]));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Cursor invalido: " + valor, e);
        }
    }
}
