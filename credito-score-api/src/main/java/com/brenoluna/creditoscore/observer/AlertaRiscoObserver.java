package com.brenoluna.creditoscore.observer;

import com.brenoluna.creditoscore.domain.Decisao;
import com.brenoluna.creditoscore.domain.ResultadoAnalise;
import com.brenoluna.creditoscore.domain.SolicitacaoCredito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Conta negativas para acompanhamento da mesa de risco.
 * Em producao viraria uma metrica (Micrometer) ou um evento de dominio.
 */
@Component
public class AlertaRiscoObserver implements AnaliseObserver {

    private static final Logger log = LoggerFactory.getLogger(AlertaRiscoObserver.class);

    private final AtomicInteger negadas = new AtomicInteger();

    @Override
    public void aoConcluirAnalise(SolicitacaoCredito solicitacao, ResultadoAnalise resultado) {
        if (resultado.decisao() == Decisao.NEGADO) {
            int total = negadas.incrementAndGet();
            log.warn("[RISCO] Analise negada (score={}). Total de negativas na instancia: {}",
                    resultado.score(), total);
        }
    }

    public int totalNegadas() {
        return negadas.get();
    }
}
