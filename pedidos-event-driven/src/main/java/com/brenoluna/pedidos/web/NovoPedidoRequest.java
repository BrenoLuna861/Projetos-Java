package com.brenoluna.pedidos.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record NovoPedidoRequest(
        @NotBlank String cliente,
        @NotEmpty @Valid List<Item> itens
) {
    public record Item(
            @NotBlank String produto,
            @Positive int quantidade,
            @NotNull @Positive BigDecimal precoUnitario
    ) {
    }
}
