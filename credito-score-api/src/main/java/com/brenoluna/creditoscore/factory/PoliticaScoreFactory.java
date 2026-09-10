package com.brenoluna.creditoscore.factory;

import com.brenoluna.creditoscore.domain.TipoProduto;
import com.brenoluna.creditoscore.regras.CapacidadePagamentoRule;
import com.brenoluna.creditoscore.regras.ComprometimentoRendaRule;
import com.brenoluna.creditoscore.regras.HistoricoPagamentoRule;
import com.brenoluna.creditoscore.regras.RegraScore;
import com.brenoluna.creditoscore.regras.RelacionamentoRule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * FACTORY - decide QUAIS regras se aplicam a cada produto.
 *
 * O servico de analise nao sabe (nem precisa saber) como uma politica e
 * montada: pede a fabrica e recebe a lista pronta. Trocar a politica de um
 * produto e mudanca de uma linha, sem risco para os demais.
 */
@Component
public class PoliticaScoreFactory {

    private final Map<TipoProduto, List<RegraScore>> politicas;

    public PoliticaScoreFactory(ComprometimentoRendaRule comprometimento,
                                HistoricoPagamentoRule historico,
                                RelacionamentoRule relacionamento,
                                CapacidadePagamentoRule capacidade) {

        this.politicas = Map.of(
                // Credito pessoal: valor baixo, o que importa e o comportamento de pagamento.
                TipoProduto.CREDITO_PESSOAL,
                List.of(comprometimento, historico, relacionamento),

                // Veiculo: entra a relacao entre valor e renda.
                TipoProduto.FINANCIAMENTO_VEICULO,
                List.of(comprometimento, historico, relacionamento, capacidade),

                // Imobiliario: mesma politica do veiculo, mas o corte de aprovacao e maior
                // (ver PoliticaCorte no servico de analise).
                TipoProduto.FINANCIAMENTO_IMOBILIARIO,
                List.of(comprometimento, historico, relacionamento, capacidade)
        );
    }

    public List<RegraScore> regrasPara(TipoProduto produto) {
        List<RegraScore> regras = politicas.get(produto);
        if (regras == null) {
            throw new IllegalArgumentException("Nenhuma politica configurada para o produto " + produto);
        }
        return regras;
    }
}
