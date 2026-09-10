package com.brenoluna.creditoscore.service;

import com.brenoluna.creditoscore.domain.Decisao;
import com.brenoluna.creditoscore.domain.ResultadoAnalise;
import com.brenoluna.creditoscore.domain.ResultadoRegra;
import com.brenoluna.creditoscore.domain.SolicitacaoCredito;
import com.brenoluna.creditoscore.domain.TipoProduto;
import com.brenoluna.creditoscore.factory.PoliticaScoreFactory;
import com.brenoluna.creditoscore.observer.AnaliseObserver;
import com.brenoluna.creditoscore.repository.AnaliseEntity;
import com.brenoluna.creditoscore.repository.AnaliseRepository;
import com.brenoluna.creditoscore.regras.RegraScore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Orquestra a analise: pega a politica do produto (Factory), aplica cada
 * regra (Strategy), persiste e avisa os interessados (Observer).
 *
 * Note que este servico nao conhece NENHUMA regra de negocio especifica.
 * Ele so sabe somar pontos e comparar com o corte.
 */
@Service
public class AnaliseCreditoService {

    /** Corte minimo de aprovacao por produto. */
    private static final Map<TipoProduto, Integer> CORTE_APROVACAO = Map.of(
            TipoProduto.CREDITO_PESSOAL, 400,
            TipoProduto.FINANCIAMENTO_VEICULO, 500,
            TipoProduto.FINANCIAMENTO_IMOBILIARIO, 600
    );

    private static final int MARGEM_RESSALVA = 100;

    private final PoliticaScoreFactory politicaFactory;
    private final AnaliseRepository repository;
    private final List<AnaliseObserver> observers;

    public AnaliseCreditoService(PoliticaScoreFactory politicaFactory,
                                 AnaliseRepository repository,
                                 List<AnaliseObserver> observers) {
        this.politicaFactory = politicaFactory;
        this.repository = repository;
        this.observers = observers;
    }

    @Transactional
    public ResultadoAnalise analisar(SolicitacaoCredito solicitacao) {
        List<RegraScore> regras = politicaFactory.regrasPara(solicitacao.produto());

        List<ResultadoRegra> detalhes = regras.stream()
                .map(regra -> regra.avaliar(solicitacao))
                .toList();

        int score = detalhes.stream().mapToInt(ResultadoRegra::pontos).sum();
        Decisao decisao = decidir(solicitacao.produto(), score);

        AnaliseEntity salva = repository.save(
                new AnaliseEntity(solicitacao.documento(), solicitacao.produto(), score, decisao));

        ResultadoAnalise resultado = new ResultadoAnalise(
                salva.getId(), solicitacao.documento(), solicitacao.produto(), score, decisao, detalhes);

        observers.forEach(o -> o.aoConcluirAnalise(solicitacao, resultado));

        return resultado;
    }

    private Decisao decidir(TipoProduto produto, int score) {
        int corte = CORTE_APROVACAO.get(produto);

        if (score >= corte) {
            return Decisao.APROVADO;
        }
        if (score >= corte - MARGEM_RESSALVA) {
            return Decisao.APROVADO_COM_RESSALVA;
        }
        return Decisao.NEGADO;
    }
}
