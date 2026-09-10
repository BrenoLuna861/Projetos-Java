package com.brenoluna.creditoscore.domain;

/**
 * Contribuicao de uma unica regra para o score final.
 *
 * @param regra   nome da regra que gerou a pontuacao
 * @param pontos  pontos atribuidos (podem ser negativos)
 * @param motivo  explicacao legivel - o que sustenta a decisao para o cliente
 */
public record ResultadoRegra(String regra, int pontos, String motivo) {
}
