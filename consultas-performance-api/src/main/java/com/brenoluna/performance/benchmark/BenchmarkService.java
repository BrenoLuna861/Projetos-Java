package com.brenoluna.performance.benchmark;

import com.brenoluna.performance.domain.Cursor;
import com.brenoluna.performance.domain.EventoAcesso;
import com.brenoluna.performance.repository.EventoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Compara as duas estrategias na MESMA base e na mesma profundidade.
 *
 * Metodo: descarta a primeira execucao (aquecimento de cache) e tira a media
 * das repeticoes seguintes. Os numeros dependem da maquina, do volume e do
 * cache do banco - por isso o resultado e devolvido pela API, e nao chutado
 * no README.
 */
@Service
public class BenchmarkService {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkService.class);

    private final EventoRepository repository;

    public BenchmarkService(EventoRepository repository) {
        this.repository = repository;
    }

    public ResultadoBenchmark comparar(int profundidade, int tamanhoPagina, int repeticoes) {
        List<ResultadoBenchmark.Medicao> medicoes = new ArrayList<>();

        // Cursor equivalente a mesma profundidade: caminha ate la uma vez, fora da medicao.
        Cursor cursor = cursorNaProfundidade(profundidade, tamanhoPagina);

        aquecer(profundidade, tamanhoPagina, cursor);

        long somaOffset = 0;
        long somaKeyset = 0;

        for (int i = 1; i <= repeticoes; i++) {
            long inicioOffset = System.nanoTime();
            List<EventoAcesso> porOffset = repository.porOffset(tamanhoPagina, profundidade);
            long duracaoOffset = (System.nanoTime() - inicioOffset) / 1_000_000;
            somaOffset += duracaoOffset;
            medicoes.add(new ResultadoBenchmark.Medicao("OFFSET", i, duracaoOffset, porOffset.size()));

            long inicioKeyset = System.nanoTime();
            List<EventoAcesso> porKeyset = repository.porKeyset(tamanhoPagina, cursor);
            long duracaoKeyset = (System.nanoTime() - inicioKeyset) / 1_000_000;
            somaKeyset += duracaoKeyset;
            medicoes.add(new ResultadoBenchmark.Medicao("KEYSET", i, duracaoKeyset, porKeyset.size()));
        }

        long mediaOffset = somaOffset / repeticoes;
        long mediaKeyset = somaKeyset / repeticoes;
        double razao = mediaKeyset == 0 ? mediaOffset : (double) mediaOffset / mediaKeyset;

        log.info("Benchmark profundidade={} offset={}ms keyset={}ms", profundidade, mediaOffset, mediaKeyset);

        return new ResultadoBenchmark(profundidade, tamanhoPagina, mediaOffset, mediaKeyset,
                Math.round(razao * 100) / 100.0, medicoes);
    }

    /**
     * Anda pela lista ate a profundidade pedida para obter um cursor honesto -
     * o mesmo ponto que o OFFSET alcancaria.
     */
    private Cursor cursorNaProfundidade(int profundidade, int tamanhoPagina) {
        if (profundidade <= 0) {
            return null;
        }
        List<EventoAcesso> ate = repository.porOffset(1, profundidade - 1);
        if (ate.isEmpty()) {
            return null;
        }
        EventoAcesso ultimo = ate.get(0);
        return new Cursor(ultimo.ocorridoEm(), ultimo.id());
    }

    private void aquecer(int profundidade, int tamanhoPagina, Cursor cursor) {
        repository.porOffset(tamanhoPagina, profundidade);
        repository.porKeyset(tamanhoPagina, cursor);
    }
}
