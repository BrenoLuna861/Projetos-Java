package com.brenoluna.pedidos.web;

import com.brenoluna.pedidos.domain.Pedido;

import java.math.BigDecimal;
import java.time.Instant;

public record PedidoResponse(Long id, String cliente, String status, BigDecimal total, Instant criadoEm) {

    public static PedidoResponse de(Pedido pedido) {
        return new PedidoResponse(
                pedido.getId(),
                pedido.getCliente(),
                pedido.getStatus().name(),
                pedido.getTotal(),
                pedido.getCriadoEm());
    }
}
