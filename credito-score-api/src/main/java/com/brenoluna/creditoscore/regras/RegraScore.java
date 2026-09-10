package com.brenoluna.creditoscore.regras;

import com.brenoluna.creditoscore.domain.ResultadoRegra;
import com.brenoluna.creditoscore.domain.SolicitacaoCredito;

/**
 * STRATEGY - cada regra de credito e uma estrategia isolada e testavel.
 *
 * Adicionar uma nova regra de negocio nao exige tocar no servico de analise:
 * basta criar uma implementacao e registra-la na politica do produto.
 */
public interface RegraScore {

    /** Nome curto usado na auditoria e na resposta da API. */
    String nome();

    /** Avalia a solicitacao e devolve os pontos que ela soma (ou subtrai). */
    ResultadoRegra avaliar(SolicitacaoCredito solicitacao);
}
