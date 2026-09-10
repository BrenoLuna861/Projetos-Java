package com.brenoluna.creditoscore.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Dados de entrada da analise. E um record imutavel: nenhuma regra
 * pode alterar a solicitacao enquanto a avalia.
 */
public record SolicitacaoCredito(
        @NotNull String documento,
        @NotNull TipoProduto produto,
        @NotNull @PositiveOrZero BigDecimal rendaMensal,
        @NotNull @PositiveOrZero BigDecimal dividasMensais,
        @NotNull @PositiveOrZero BigDecimal valorSolicitado,
        @Min(18) int idade,
        @PositiveOrZero int atrasos12Meses,
        @PositiveOrZero int mesesRelacionamento
) {

    /** Percentual da renda ja comprometido com dividas (0.0 a 1.0+). */
    public BigDecimal comprometimentoRenda() {
        if (rendaMensal.signum() == 0) {
            return BigDecimal.ONE;
        }
        return dividasMensais.divide(rendaMensal, 4, java.math.RoundingMode.HALF_UP);
    }
}
