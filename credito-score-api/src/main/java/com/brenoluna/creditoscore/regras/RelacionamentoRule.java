package com.brenoluna.creditoscore.regras;

import com.brenoluna.creditoscore.domain.ResultadoRegra;
import com.brenoluna.creditoscore.domain.SolicitacaoCredito;
import org.springframework.stereotype.Component;

/** Tempo de relacionamento com a instituicao - cliente antigo tem historico conhecido. */
@Component
public class RelacionamentoRule implements RegraScore {

    @Override
    public String nome() {
        return "TEMPO_RELACIONAMENTO";
    }

    @Override
    public ResultadoRegra avaliar(SolicitacaoCredito s) {
        int meses = s.mesesRelacionamento();

        if (meses >= 60) {
            return new ResultadoRegra(nome(), 150, "Cliente ha %d meses".formatted(meses));
        }
        if (meses >= 12) {
            return new ResultadoRegra(nome(), 80, "Cliente ha %d meses".formatted(meses));
        }
        return new ResultadoRegra(nome(), 0, "Relacionamento recente (%d meses)".formatted(meses));
    }
}
