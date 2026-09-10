package com.brenoluna.creditoscore.repository;

import com.brenoluna.creditoscore.domain.Decisao;
import com.brenoluna.creditoscore.domain.TipoProduto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "analise_credito")
public class AnaliseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String documento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoProduto produto;

    @Column(nullable = false)
    private int score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Decisao decisao;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    protected AnaliseEntity() {
        // exigido pelo JPA
    }

    public AnaliseEntity(String documento, TipoProduto produto, int score, Decisao decisao) {
        this.documento = documento;
        this.produto = produto;
        this.score = score;
        this.decisao = decisao;
    }

    public Long getId() {
        return id;
    }

    public String getDocumento() {
        return documento;
    }

    public TipoProduto getProduto() {
        return produto;
    }

    public int getScore() {
        return score;
    }

    public Decisao getDecisao() {
        return decisao;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
