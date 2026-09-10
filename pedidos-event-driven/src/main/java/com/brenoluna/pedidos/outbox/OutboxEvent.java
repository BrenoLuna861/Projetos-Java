package com.brenoluna.pedidos.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Linha da tabela de OUTBOX.
 *
 * O evento e gravado na MESMA transacao do pedido. Se a transacao der rollback,
 * o evento some junto - nunca existe evento publicado para um pedido que nao
 * foi salvo, nem pedido salvo cujo evento se perdeu porque o Kafka estava fora.
 */
@Entity
@Table(name = "outbox_event", indexes = {
        @Index(name = "idx_outbox_status_criado", columnList = "status, criado_em")
})
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tipo do agregado de origem, ex.: "PEDIDO". */
    @Column(name = "tipo_agregado", nullable = false, length = 40)
    private String tipoAgregado;

    /** Id do agregado - vira a chave da mensagem no Kafka, garantindo ordem por pedido. */
    @Column(name = "id_agregado", nullable = false, length = 40)
    private String idAgregado;

    /** Nome do evento, ex.: "PedidoCriado". */
    @Column(name = "tipo_evento", nullable = false, length = 60)
    private String tipoEvento;

    @Lob
    @Column(nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusOutbox status = StatusOutbox.PENDENTE;

    @Column(nullable = false)
    private int tentativas = 0;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(name = "publicado_em")
    private Instant publicadoEm;

    @Column(name = "ultimo_erro", length = 500)
    private String ultimoErro;

    protected OutboxEvent() {
    }

    public OutboxEvent(String tipoAgregado, String idAgregado, String tipoEvento, String payload) {
        this.tipoAgregado = tipoAgregado;
        this.idAgregado = idAgregado;
        this.tipoEvento = tipoEvento;
        this.payload = payload;
    }

    public void marcarPublicado() {
        this.status = StatusOutbox.PUBLICADO;
        this.publicadoEm = Instant.now();
        this.ultimoErro = null;
    }

    public void registrarFalha(String erro, int maxTentativas) {
        this.tentativas++;
        this.ultimoErro = erro != null && erro.length() > 500 ? erro.substring(0, 500) : erro;
        if (this.tentativas >= maxTentativas) {
            this.status = StatusOutbox.FALHA;
        }
    }

    public Long getId() {
        return id;
    }

    public String getTipoAgregado() {
        return tipoAgregado;
    }

    public String getIdAgregado() {
        return idAgregado;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public String getPayload() {
        return payload;
    }

    public StatusOutbox getStatus() {
        return status;
    }

    public int getTentativas() {
        return tentativas;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getPublicadoEm() {
        return publicadoEm;
    }

    public String getUltimoErro() {
        return ultimoErro;
    }
}
