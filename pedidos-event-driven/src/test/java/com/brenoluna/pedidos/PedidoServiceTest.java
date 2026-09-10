package com.brenoluna.pedidos;

import com.brenoluna.pedidos.domain.Pedido;
import com.brenoluna.pedidos.outbox.OutboxEvent;
import com.brenoluna.pedidos.outbox.OutboxRepository;
import com.brenoluna.pedidos.outbox.StatusOutbox;
import com.brenoluna.pedidos.service.PedidoService;
import com.brenoluna.pedidos.web.NovoPedidoRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@ActiveProfiles("test")
class PedidoServiceTest {

    @Autowired
    private PedidoService service;

    @Autowired
    private OutboxRepository outboxRepository;

    private NovoPedidoRequest pedidoDeExemplo() {
        return new NovoPedidoRequest("Maria Souza", List.of(
                new NovoPedidoRequest.Item("Teclado", 2, new BigDecimal("150.00")),
                new NovoPedidoRequest.Item("Monitor", 1, new BigDecimal("900.00"))));
    }

    @Test
    @DisplayName("Criar pedido grava o evento na outbox na mesma transacao")
    void criaPedidoEEvento() {
        long antes = outboxRepository.count();

        Pedido pedido = service.criar(pedidoDeExemplo());

        assertThat(pedido.getId()).isNotNull();
        assertThat(pedido.getTotal()).isEqualByComparingTo("1200.00");
        assertThat(outboxRepository.count()).isEqualTo(antes + 1);

        OutboxEvent evento = outboxRepository.findAll().stream()
                .filter(e -> e.getIdAgregado().equals(String.valueOf(pedido.getId())))
                .findFirst()
                .orElseThrow();

        assertThat(evento.getStatus()).isEqualTo(StatusOutbox.PENDENTE);
        assertThat(evento.getTipoEvento()).isEqualTo("PedidoCriado");
        assertThat(evento.getPayload()).contains("\"pedidoId\":" + pedido.getId());
        assertThat(evento.getTentativas()).isZero();
    }

    @Test
    @DisplayName("Total do pedido e a soma dos subtotais dos itens")
    void calculaTotal() {
        Pedido pedido = service.criar(new NovoPedidoRequest("Joao", List.of(
                new NovoPedidoRequest.Item("Cabo HDMI", 3, new BigDecimal("39.90")))));

        assertThat(pedido.getTotal()).isEqualByComparingTo("119.70");
    }
}
