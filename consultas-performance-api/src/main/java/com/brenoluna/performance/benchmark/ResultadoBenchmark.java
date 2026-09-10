package com.brenoluna.performance.benchmark;

import java.util.List;

public record ResultadoBenchmark(
        int profundidade,
        int tamanhoPagina,
        long offsetMediaMs,
        long keysetMediaMs,
        double vezesMaisRapido,
        List<Medicao> medicoes
) {
    public record Medicao(String estrategia, int repeticao, long duracaoMs, int linhas) {
    }
}
