package com.brenoluna.creditoscore;

import com.brenoluna.creditoscore.domain.Decisao;
import com.brenoluna.creditoscore.domain.SolicitacaoCredito;
import com.brenoluna.creditoscore.domain.TipoProduto;
import com.brenoluna.creditoscore.service.AnaliseCreditoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AnaliseCreditoServiceTest {

    @Autowired
    private AnaliseCreditoService service;

    @Test
    @DisplayName("Bom pagador com renda folgada e aprovado no credito pessoal")
    void aprovado() {
        var solicitacao = new SolicitacaoCredito(
                "12345678901", TipoProduto.CREDITO_PESSOAL,
                new BigDecimal("6000"), new BigDecimal("1200"), new BigDecimal("4000"),
                35, 0, 48);

        var resultado = service.analisar(solicitacao);

        assertThat(resultado.decisao()).isEqualTo(Decisao.APROVADO);
        assertThat(resultado.id()).isNotNull();
        assertThat(resultado.detalhes()).hasSize(3);
    }

    @Test
    @DisplayName("Renda comprometida e historico ruim levam a negativa")
    void negado() {
        var solicitacao = new SolicitacaoCredito(
                "98765432100", TipoProduto.CREDITO_PESSOAL,
                new BigDecimal("2000"), new BigDecimal("1600"), new BigDecimal("10000"),
                22, 4, 2);

        var resultado = service.analisar(solicitacao);

        assertThat(resultado.decisao()).isEqualTo(Decisao.NEGADO);
    }

    @Test
    @DisplayName("Financiamento imobiliario aplica a regra de capacidade de pagamento")
    void politicaImobiliaria() {
        var solicitacao = new SolicitacaoCredito(
                "11122233344", TipoProduto.FINANCIAMENTO_IMOBILIARIO,
                new BigDecimal("12000"), new BigDecimal("2000"), new BigDecimal("300000"),
                40, 0, 90);

        var resultado = service.analisar(solicitacao);

        assertThat(resultado.detalhes())
                .extracting("regra")
                .contains("CAPACIDADE_PAGAMENTO");
    }
}
