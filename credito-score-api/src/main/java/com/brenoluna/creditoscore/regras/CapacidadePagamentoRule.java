package com.brenoluna.creditoscore.regras;

import com.brenoluna.creditoscore.domain.ResultadoRegra;
import com.brenoluna.creditoscore.domain.SolicitacaoCredito;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Relacao entre o valor solicitado e a renda anual.
 * Usada nas politicas de valor alto (veiculo e imobiliario).
 */
@Component
public class CapacidadePagamentoRule implements RegraScore {

    private static final BigDecimal MESES_ANO = BigDecimal.valueOf(12);

    @Override
    public String nome() {
        return "CAPACIDADE_PAGAMENTO";
    }

    @Override
    public ResultadoRegra avaliar(SolicitacaoCredito s) {
        BigDecimal rendaAnual = s.rendaMensal().multiply(MESES_ANO);

        if (rendaAnual.signum() == 0) {
            return new ResultadoRegra(nome(), -200, "Renda declarada igual a zero");
        }

        BigDecimal proporcao = s.valorSolicitado().divide(rendaAnual, 2, RoundingMode.HALF_UP);

        if (proporcao.compareTo(BigDecimal.ONE) <= 0) {
            return new ResultadoRegra(nome(), 200,
                    "Valor solicitado equivale a %sx a renda anual".formatted(proporcao.toPlainString()));
        }
        if (proporcao.compareTo(BigDecimal.valueOf(3)) <= 0) {
            return new ResultadoRegra(nome(), 60,
                    "Valor solicitado equivale a %sx a renda anual".formatted(proporcao.toPlainString()));
        }
        return new ResultadoRegra(nome(), -120,
                "Valor solicitado equivale a %sx a renda anual".formatted(proporcao.toPlainString()));
    }
}
