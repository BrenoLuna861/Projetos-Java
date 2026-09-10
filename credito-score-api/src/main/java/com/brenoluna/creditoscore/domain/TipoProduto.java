package com.brenoluna.creditoscore.domain;

/**
 * Produto de credito solicitado. Cada produto tem uma politica de score
 * diferente - ver {@link com.brenoluna.creditoscore.factory.PoliticaScoreFactory}.
 */
public enum TipoProduto {
    CREDITO_PESSOAL,
    FINANCIAMENTO_VEICULO,
    FINANCIAMENTO_IMOBILIARIO
}
