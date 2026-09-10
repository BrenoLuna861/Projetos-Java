package com.brenoluna.creditoscore.regras;

import com.brenoluna.creditoscore.domain.ResultadoRegra;
import com.brenoluna.creditoscore.domain.SolicitacaoCredito;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Quanto da renda ja esta comprometido com outras dividas.
 * E a regra de maior peso em qualquer politica.
 */
@Component
public class ComprometimentoRendaRule implements RegraScore {

    private static final BigDecimal LIMITE_SAUDAVEL = new BigDecimal("0.30");
    private static final BigDecimal LIMITE_ATENCAO = new BigDecimal("0.50");

    @Override
    public String nome() {
        return "COMPROMETIMENTO_RENDA";
    }

    @Override
    public ResultadoRegra avaliar(SolicitacaoCredito s) {
        BigDecimal comprometimento = s.comprometimentoRenda();

        if (comprometimento.compareTo(LIMITE_SAUDAVEL) <= 0) {
            return new ResultadoRegra(nome(), 300,
                    "Comprometimento de renda em %s%%, dentro do limite saudavel de 30%%"
                            .formatted(percentual(comprometimento)));
        }
        if (comprometimento.compareTo(LIMITE_ATENCAO) <= 0) {
            return new ResultadoRegra(nome(), 120,
                    "Comprometimento de renda em %s%%, acima do ideal mas tolerado"
                            .formatted(percentual(comprometimento)));
        }
        return new ResultadoRegra(nome(), -150,
                "Comprometimento de renda em %s%%, acima do limite de 50%%"
                        .formatted(percentual(comprometimento)));
    }

    private String percentual(BigDecimal valor) {
        return valor.multiply(BigDecimal.valueOf(100)).setScale(1, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
