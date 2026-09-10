package com.brenoluna.pedidos.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Le a outbox e publica no Kafka.
 *
 * Roda separado da transacao de negocio: se o broker estiver fora, o pedido
 * ja foi salvo e o evento continua PENDENTE, sendo reenviado na proxima rodada.
 * A entrega e "pelo menos uma vez" - por isso a chave da mensagem e o id do
 * agregado, permitindo que o consumidor seja idempotente.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.outbox.topico:pedidos.eventos}")
    private String topico;

    @Value("${app.outbox.lote:50}")
    private int tamanhoLote;

    @Value("${app.outbox.max-tentativas:5}")
    private int maxTentativas;

    @Value("${app.outbox.timeout-segundos:10}")
    private long timeoutSegundos;

    public OutboxPublisher(OutboxRepository repository, KafkaTemplate<String, String> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${app.outbox.intervalo-ms:2000}")
    @Transactional
    public void publicarPendentes() {
        List<OutboxEvent> pendentes =
                repository.findByStatusOrderByCriadoEmAsc(StatusOutbox.PENDENTE, Limit.of(tamanhoLote));

        if (pendentes.isEmpty()) {
            return;
        }

        log.debug("Publicando {} evento(s) da outbox", pendentes.size());

        for (OutboxEvent evento : pendentes) {
            try {
                kafkaTemplate
                        .send(topico, evento.getIdAgregado(), evento.getPayload())
                        .get(timeoutSegundos, TimeUnit.SECONDS);

                evento.marcarPublicado();
                log.info("Evento {} ({}) publicado no topico {}",
                        evento.getId(), evento.getTipoEvento(), topico);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                evento.registrarFalha("Publicacao interrompida", maxTentativas);
                return;
            } catch (Exception e) {
                evento.registrarFalha(e.getMessage(), maxTentativas);
                log.warn("Falha ao publicar evento {} (tentativa {}): {}",
                        evento.getId(), evento.getTentativas(), e.getMessage());
            }
        }
    }
}
