package com.brenoluna.creditoscore.observer;

import com.brenoluna.creditoscore.domain.ResultadoAnalise;
import com.brenoluna.creditoscore.domain.SolicitacaoCredito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Registra toda analise para rastreabilidade - exigencia comum em credito. */
@Component
public class AuditoriaObserver implements AnaliseObserver {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaObserver.class);

    @Override
    public void aoConcluirAnalise(SolicitacaoCredito solicitacao, ResultadoAnalise resultado) {
        log.info("[AUDITORIA] documento={} produto={} score={} decisao={} regras={}",
                mascarar(solicitacao.documento()),
                solicitacao.produto(),
                resultado.score(),
                resultado.decisao(),
                resultado.detalhes().size());
    }

    /** Nunca logar documento completo. */
    private String mascarar(String documento) {
        if (documento == null || documento.length() <= 4) {
            return "***";
        }
        return "***" + documento.substring(documento.length() - 4);
    }
}
