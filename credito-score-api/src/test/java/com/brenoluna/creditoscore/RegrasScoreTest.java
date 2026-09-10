package com.brenoluna.creditoscore;

import com.brenoluna.creditoscore.domain.SolicitacaoCredito;
import com.brenoluna.creditoscore.domain.TipoProduto;
import com.brenoluna.creditoscore.regras.ComprometimentoRendaRule;
import com.brenoluna.creditoscore.regras.HistoricoPagamentoRule;
import com.brenoluna.creditoscore.regras.RelacionamentoRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RegrasScoreTest {

    private SolicitacaoCredito solicitacao(String renda, String dividas, int atrasos, int meses) {
        return new SolicitacaoCredito(
                "12345678901",
                TipoProduto.CREDITO_PESSOAL,
                new BigDecimal(renda),
                new BigDecimal(dividas),
                new BigDecimal("5000"),
                30,
                atrasos,
                meses);
    }

    @Test
    @DisplayName("Renda pouco comprometida soma a pontuacao maxima da regra")
    void rendaSaudavel() {
        var regra = new ComprometimentoRendaRule();
        var resultado = regra.avaliar(solicitacao("5000", "1000", 0, 24));

        assertThat(resultado.pontos()).isEqualTo(300);
        assertThat(resultado.motivo()).contains("20.0%");
    }

    @Test
    @DisplayName("Renda muito comprometida subtrai pontos")
    void rendaComprometida() {
        var regra = new ComprometimentoRendaRule();
        var resultado = regra.avaliar(solicitacao("3000", "2000", 0, 24));

        assertThat(resultado.pontos()).isNegative();
    }

    @Test
    @DisplayName("Renda zerada nao quebra a divisao e e tratada como risco maximo")
    void rendaZerada() {
        var regra = new ComprometimentoRendaRule();
        var resultado = regra.avaliar(solicitacao("0", "500", 0, 10));

        assertThat(resultado.pontos()).isEqualTo(-150);
    }

    @Test
    @DisplayName("Cada atraso reduz pontos, respeitando o piso")
    void historicoComAtrasos() {
        var regra = new HistoricoPagamentoRule();

        assertThat(regra.avaliar(solicitacao("5000", "1000", 0, 24)).pontos()).isEqualTo(250);
        assertThat(regra.avaliar(solicitacao("5000", "1000", 2, 24)).pontos()).isEqualTo(70);
        assertThat(regra.avaliar(solicitacao("5000", "1000", 99, 24)).pontos()).isEqualTo(-200);
    }

    @Test
    @DisplayName("Relacionamento longo vale mais que relacionamento recente")
    void relacionamento() {
        var regra = new RelacionamentoRule();

        int antigo = regra.avaliar(solicitacao("5000", "1000", 0, 72)).pontos();
        int recente = regra.avaliar(solicitacao("5000", "1000", 0, 3)).pontos();

        assertThat(antigo).isGreaterThan(recente);
    }
}
