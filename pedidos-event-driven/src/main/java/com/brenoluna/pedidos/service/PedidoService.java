package com.brenoluna.pedidos.service;

import com.brenoluna.pedidos.domain.Pedido;
import com.brenoluna.pedidos.domain.PedidoRepository;
import com.brenoluna.pedidos.outbox.OutboxEvent;
import com.brenoluna.pedidos.outbox.OutboxRepository;
import com.brenoluna.pedidos.web.NovoPedidoRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * O coracao do padrao: pedido e evento sao gravados na MESMA transacao.
 *
 * Nao ha chamada ao Kafka aqui. Publicar dentro da transacao seria o erro
 * classico - a mensagem pode sair e o commit falhar depois, criando um evento
 * de um pedido que nao existe.
 */
@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public PedidoService(PedidoRepository pedidoRepository,
                         OutboxRepository outboxRepository,
                         ObjectMapper objectMapper) {
        this.pedidoRepository = pedidoRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Pedido criar(NovoPedidoRequest request) {
        Pedido pedido = new Pedido(request.cliente());
        request.itens().forEach(i -> pedido.adicionarItem(i.produto(), i.quantidade(), i.precoUnitario()));

        Pedido salvo = pedidoRepository.save(pedido);

        outboxRepository.save(new OutboxEvent(
                "PEDIDO",
                String.valueOf(salvo.getId()),
                "PedidoCriado",
                serializar(salvo)));

        return salvo;
    }

    private String serializar(Pedido pedido) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "evento", "PedidoCriado",
                    "pedidoId", pedido.getId(),
                    "cliente", pedido.getCliente(),
                    "total", pedido.getTotal(),
                    "status", pedido.getStatus().name(),
                    "criadoEm", pedido.getCriadoEm().toString()));
        } catch (JsonProcessingException e) {
            // Falhar aqui derruba a transacao inteira - e o comportamento correto:
            // melhor nao criar o pedido do que criar sem o evento correspondente.
            throw new IllegalStateException("Falha ao serializar evento do pedido", e);
        }
    }
}
