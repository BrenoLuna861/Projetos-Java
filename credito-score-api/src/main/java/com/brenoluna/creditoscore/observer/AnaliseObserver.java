package com.brenoluna.creditoscore.observer;

import com.brenoluna.creditoscore.domain.ResultadoAnalise;
import com.brenoluna.creditoscore.domain.SolicitacaoCredito;

/**
 * OBSERVER - reage a uma analise concluida sem que o servico saiba quem escuta.
 *
 * Auditoria, alerta de risco e (no futuro) envio de notificacao sao
 * preocupacoes separadas do calculo do score.
 */
public interface AnaliseObserver {
    void aoConcluirAnalise(SolicitacaoCredito solicitacao, ResultadoAnalise resultado);
}
