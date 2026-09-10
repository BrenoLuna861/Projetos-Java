package com.brenoluna.performance;

import com.brenoluna.performance.domain.Cursor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CursorTest {

    @Test
    @DisplayName("Cursor codificado e decodificado devolve os mesmos valores")
    void idaEVolta() {
        var original = new Cursor(LocalDateTime.of(2026, 3, 14, 10, 30, 45), 987654L);

        var decodificado = Cursor.decodificar(original.codificar());

        assertThat(decodificado).isEqualTo(original);
    }

    @Test
    @DisplayName("Cursor nao expoe o timestamp em texto puro na URL")
    void naoVazaFormatoInterno() {
        var cursor = new Cursor(LocalDateTime.of(2026, 1, 1, 0, 0), 1L);

        assertThat(cursor.codificar()).doesNotContain("2026-01-01");
    }

    @Test
    @DisplayName("Cursor invalido gera erro claro em vez de quebrar a consulta")
    void cursorInvalido() {
        assertThatThrownBy(() -> Cursor.decodificar("nao-e-base64-valido!!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cursor invalido");
    }

    @Test
    @DisplayName("Cursor codificado e seguro para URL (sem + / ou =)")
    void seguroParaUrl() {
        var cursor = new Cursor(LocalDateTime.of(2026, 12, 31, 23, 59, 59), Long.MAX_VALUE);

        assertThat(cursor.codificar()).doesNotContain("+", "/", "=");
    }
}
