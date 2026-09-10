package com.brenoluna.pedidos.outbox;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Busca os eventos ainda nao publicados, do mais antigo para o mais novo,
     * limitando o lote para nao carregar a tabela inteira em memoria.
     */
    List<OutboxEvent> findByStatusOrderByCriadoEmAsc(StatusOutbox status, Limit limit);

    long countByStatus(StatusOutbox status);
}
