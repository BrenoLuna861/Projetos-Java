package com.brenoluna.pedidos.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consumidor de exemplo. Existe para demonstrar o outro lado do fluxo e,
 * principalmente, a IDEMPOTENCIA: como a outbox entrega pelo menos uma vez,
 * o consumidor precisa aguentar receber a mesma mensagem duas vezes.
 */
@Component
public class PedidoEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventConsumer.class);

    /** Em producao isso seria uma tabela de mensagens processadas, nao memoria. */
    private final Set<String> processados = ConcurrentHashMap.newKeySet();

    @KafkaListener(
            topics = "${app.outbox.topico:pedidos.eventos}",
            groupId = "${spring.kafka.consumer.group-id:pedidos-consumer}")
    public void consumir(String chave, String payload) {
        if (!processados.add(chave + "|" + payload.hashCode())) {
            log.info("Evento duplicado do pedido {} ignorado", chave);
            return;
        }
        log.info("Evento recebido do pedido {}: {}", chave, payload);
    }

    public int totalProcessados() {
        return processados.size();
    }
}
