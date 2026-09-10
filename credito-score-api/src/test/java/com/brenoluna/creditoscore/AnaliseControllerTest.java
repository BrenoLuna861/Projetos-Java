package com.brenoluna.creditoscore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AnaliseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("POST /api/v1/analises devolve 201 com score e detalhes")
    void analisaComSucesso() throws Exception {
        String body = """
                {
                  "documento": "12345678901",
                  "produto": "CREDITO_PESSOAL",
                  "rendaMensal": 6000,
                  "dividasMensais": 1200,
                  "valorSolicitado": 4000,
                  "idade": 35,
                  "atrasos12Meses": 0,
                  "mesesRelacionamento": 48
                }
                """;

        mockMvc.perform(post("/api/v1/analises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.decisao").value("APROVADO"))
                .andExpect(jsonPath("$.detalhes.length()").value(3));
    }

    @Test
    @DisplayName("Payload invalido devolve 400 com o campo problematico")
    void validaEntrada() throws Exception {
        String body = """
                {
                  "documento": "12345678901",
                  "produto": "CREDITO_PESSOAL",
                  "rendaMensal": 6000,
                  "dividasMensais": 1200,
                  "valorSolicitado": 4000,
                  "idade": 15,
                  "atrasos12Meses": 0,
                  "mesesRelacionamento": 48
                }
                """;

        mockMvc.perform(post("/api/v1/analises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.idade").exists());
    }
}
