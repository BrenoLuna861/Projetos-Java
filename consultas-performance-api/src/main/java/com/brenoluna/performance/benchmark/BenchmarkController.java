package com.brenoluna.performance.benchmark;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/benchmark")
public class BenchmarkController {

    private final BenchmarkService service;

    public BenchmarkController(BenchmarkService service) {
        this.service = service;
    }

    /**
     * Exemplo:
     * GET /api/v1/benchmark?profundidade=400000&tamanho=20&repeticoes=5
     */
    @GetMapping
    public ResultadoBenchmark comparar(@RequestParam(defaultValue = "100000") int profundidade,
                                       @RequestParam(defaultValue = "20") int tamanho,
                                       @RequestParam(defaultValue = "5") int repeticoes) {
        return service.comparar(profundidade, tamanho, repeticoes);
    }

    /** Roda a comparacao em varias profundidades de uma vez. */
    @GetMapping("/curva")
    public List<ResultadoBenchmark> curva(@RequestParam(defaultValue = "20") int tamanho,
                                          @RequestParam(defaultValue = "3") int repeticoes) {
        return List.of(0, 1_000, 10_000, 100_000, 250_000, 400_000).stream()
                .map(p -> service.comparar(p, tamanho, repeticoes))
                .toList();
    }
}
