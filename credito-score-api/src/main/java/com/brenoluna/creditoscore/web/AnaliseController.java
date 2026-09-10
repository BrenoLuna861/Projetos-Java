package com.brenoluna.creditoscore.web;

import com.brenoluna.creditoscore.domain.ResultadoAnalise;
import com.brenoluna.creditoscore.domain.SolicitacaoCredito;
import com.brenoluna.creditoscore.service.AnaliseCreditoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analises")
public class AnaliseController {

    private final AnaliseCreditoService service;

    public AnaliseController(AnaliseCreditoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ResultadoAnalise> analisar(@Valid @RequestBody SolicitacaoCredito solicitacao) {
        ResultadoAnalise resultado = service.analisar(solicitacao);
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }
}
