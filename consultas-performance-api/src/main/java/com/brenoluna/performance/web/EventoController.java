package com.brenoluna.performance.web;

import com.brenoluna.performance.domain.Cursor;
import com.brenoluna.performance.domain.EventoAcesso;
import com.brenoluna.performance.repository.EventoRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/eventos")
public class EventoController {

    private static final int TAMANHO_MAXIMO = 200;

    private final EventoRepository repository;

    public EventoController(EventoRepository repository) {
        this.repository = repository;
    }

    /** Paginacao classica por OFFSET - mantida para comparacao. */
    @GetMapping("/offset")
    public PaginaResponse porOffset(@RequestParam(defaultValue = "0") int pagina,
                                    @RequestParam(defaultValue = "20") int tamanho) {
        int limite = Math.min(tamanho, TAMANHO_MAXIMO);
        long inicio = System.nanoTime();
        List<EventoAcesso> itens = repository.porOffset(limite, pagina * limite);
        long duracao = (System.nanoTime() - inicio) / 1_000_000;

        return new PaginaResponse(itens, itens.size(), null, duracao);
    }

    /** Paginacao por KEYSET - a que deve ir para producao. */
    @GetMapping("/keyset")
    public PaginaResponse porKeyset(@RequestParam(required = false) String cursor,
                                    @RequestParam(defaultValue = "20") int tamanho) {
        int limite = Math.min(tamanho, TAMANHO_MAXIMO);
        Cursor posicao = (cursor == null || cursor.isBlank()) ? null : Cursor.decodificar(cursor);

        long inicio = System.nanoTime();
        List<EventoAcesso> itens = repository.porKeyset(limite, posicao);
        long duracao = (System.nanoTime() - inicio) / 1_000_000;

        String proximo = itens.isEmpty()
                ? null
                : new Cursor(itens.get(itens.size() - 1).ocorridoEm(),
                             itens.get(itens.size() - 1).id()).codificar();

        return new PaginaResponse(itens, itens.size(), proximo, duracao);
    }

    @GetMapping("/total")
    public long total() {
        return repository.total();
    }
}
