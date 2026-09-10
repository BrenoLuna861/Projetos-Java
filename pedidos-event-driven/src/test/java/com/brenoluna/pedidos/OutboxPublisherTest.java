package com.brenoluna.pedidos;

import com.brenoluna.pedidos.outbox.OutboxEvent;
import com.brenoluna.pedidos.outbox.OutboxPublisher;
import com.brenoluna.pedidos.outbox.OutboxRepository;
import com.brenoluna.pedidos.outbox.StatusOutbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@ActiveProfiles("test")
class OutboxPublisherTest {

    @Autowired
    private OutboxPublisher publisher;

    @Autowired
    private OutboxRepository repository;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void limpar() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("Evento publicado com sucesso muda para PUBLICADO")
    void publicaComSucesso() {
        @SuppressWarnings("unchecked")
        SendResult<String, String> sendResult = mock(SendResult.class);
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        OutboxEvent evento = repository.save(
                new OutboxEvent("PEDIDO", "1", "PedidoCriado", "{\"pedidoId\":1}"));

        publisher.publicarPendentes();

        OutboxEvent atualizado = repository.findById(evento.getId()).orElseThrow();
        assertThat(atualizado.getStatus()).isEqualTo(StatusOutbox.PUBLICADO);
        assertThat(atualizado.getPublicadoEm()).isNotNull();
    }

    @Test
    @DisplayName("Broker fora do ar mantem o evento PENDENTE e conta a tentativa")
    void mantemPendenteQuandoFalha() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker indisponivel")));

        OutboxEvent evento = repository.save(
                new OutboxEvent("PEDIDO", "2", "PedidoCriado", "{\"pedidoId\":2}"));

        publisher.publicarPendentes();

        OutboxEvent atualizado = repository.findById(evento.getId()).orElseThrow();
        assertThat(atualizado.getStatus()).isEqualTo(StatusOutbox.PENDENTE);
        assertThat(atualizado.getTentativas()).isEqualTo(1);
        assertThat(atualizado.getUltimoErro()).contains("broker indisponivel");
    }

    @Test
    @DisplayName("Depois do limite de tentativas o evento vai para FALHA")
    void marcaFalhaAposLimite() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker indisponivel")));

        OutboxEvent evento = repository.save(
                new OutboxEvent("PEDIDO", "3", "PedidoCriado", "{\"pedidoId\":3}"));

        for (int i = 0; i < 5; i++) {
            publisher.publicarPendentes();
        }

        OutboxEvent atualizado = repository.findById(evento.getId()).orElseThrow();
        assertThat(atualizado.getStatus()).isEqualTo(StatusOutbox.FALHA);
        assertThat(atualizado.getTentativas()).isEqualTo(5);
    }
}
