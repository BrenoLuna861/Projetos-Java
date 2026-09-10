package com.brenoluna.performance.web;

import com.brenoluna.performance.domain.EventoAcesso;

import java.util.List;

public record PaginaResponse(
        List<EventoAcesso> itens,
        int tamanho,
        String proximoCursor,
        long duracaoMs
) {
}
