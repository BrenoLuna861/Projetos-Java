package com.brenoluna.creditoscore.regras;

import com.brenoluna.creditoscore.domain.ResultadoRegra;
import com.brenoluna.creditoscore.domain.SolicitacaoCredito;
import org.springframework.stereotype.Component;

/** Atrasos nos ultimos 12 meses. Cada atraso custa pontos, com piso. */
@Component
public class HistoricoPagamentoRule implements RegraScore {

    private static final int PONTOS_BASE = 250;
    private static final int PENALIDADE_POR_ATRASO = 90;
    private static final int PISO = -200;

    @Override
    public String nome() {
        return "HISTORICO_PAGAMENTO";
    }

    @Override
    public ResultadoRegra avaliar(SolicitacaoCredito s) {
        int pontos = Math.max(PISO, PONTOS_BASE - (s.atrasos12Meses() * PENALIDADE_POR_ATRASO));

        String motivo = s.atrasos12Meses() == 0
                ? "Nenhum atraso registrado nos ultimos 12 meses"
                : "%d atraso(s) nos ultimos 12 meses".formatted(s.atrasos12Meses());

        return new ResultadoRegra(nome(), pontos, motivo);
    }
}
