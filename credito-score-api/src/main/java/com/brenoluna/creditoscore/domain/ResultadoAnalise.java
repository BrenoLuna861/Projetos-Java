package com.brenoluna.creditoscore.domain;

import java.util.List;

public record ResultadoAnalise(
        Long id,
        String documento,
        TipoProduto produto,
        int score,
        Decisao decisao,
        List<ResultadoRegra> detalhes
) {
}
