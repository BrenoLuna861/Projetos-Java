package com.brenoluna.pedidos.web;

import com.brenoluna.pedidos.domain.Pedido;
import com.brenoluna.pedidos.domain.PedidoRepository;
import com.brenoluna.pedidos.outbox.OutboxRepository;
import com.brenoluna.pedidos.outbox.StatusOutbox;
import com.brenoluna.pedidos.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoController {

    private final PedidoService service;
    private final PedidoRepository pedidoRepository;
    private final OutboxRepository outboxRepository;

    public PedidoController(PedidoService service,
                            PedidoRepository pedidoRepository,
                            OutboxRepository outboxRepository) {
        this.service = service;
        this.pedidoRepository = pedidoRepository;
        this.outboxRepository = outboxRepository;
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> criar(@Valid @RequestBody NovoPedidoRequest request) {
        Pedido pedido = service.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(PedidoResponse.de(pedido));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> buscar(@PathVariable Long id) {
        return pedidoRepository.findById(id)
                .map(PedidoResponse::de)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Endpoint de observabilidade: mostra o estado da fila de eventos. */
    @GetMapping("/outbox/status")
    public Map<String, Long> statusOutbox() {
        return Map.of(
                "pendentes", outboxRepository.countByStatus(StatusOutbox.PENDENTE),
                "publicados", outboxRepository.countByStatus(StatusOutbox.PUBLICADO),
                "falhas", outboxRepository.countByStatus(StatusOutbox.FALHA));
    }
}
