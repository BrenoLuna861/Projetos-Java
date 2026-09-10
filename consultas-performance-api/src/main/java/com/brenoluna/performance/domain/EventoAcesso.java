package com.brenoluna.performance.domain;

import java.time.LocalDateTime;

public record EventoAcesso(
        long id,
        long usuarioId,
        String acao,
        String ip,
        LocalDateTime ocorridoEm
) {
}
