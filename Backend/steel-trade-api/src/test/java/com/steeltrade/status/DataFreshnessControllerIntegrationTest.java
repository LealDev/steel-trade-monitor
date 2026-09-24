package com.steeltrade.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.steeltrade.TestcontainersConfiguration;
import com.steeltrade.ingestion.core.IngestionLog;
import com.steeltrade.ingestion.core.IngestionLogRepository;
import com.steeltrade.ingestion.core.StatusExecucao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Teste de rota do freshness contra banco real. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DataFreshnessControllerIntegrationTest {

    private static final OffsetDateTime BASE = OffsetDateTime.of(2026, 9, 22, 3, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IngestionLogRepository ingestionLogRepository;

    @BeforeEach
    void limpar() {
        ingestionLogRepository.deleteAll();
    }

    @Test
    void devolveUltimaExecucaoEUltimoSucessoPorFonte() throws Exception {
        var sucesso = IngestionLog.iniciar("COMEXSTAT", "2025-01..2025-06", BASE.minusDays(1));
        sucesso.finalizar(StatusExecucao.SUCESSO, 988, 988, null, BASE.minusDays(1).plusMinutes(2));
        ingestionLogRepository.save(sucesso);

        var falha = IngestionLog.iniciar("COMEXSTAT", "2025-01..2025-06", BASE);
        falha.finalizar(StatusExecucao.FALHA, null, null, "Fonte respondeu HTTP 429", BASE.plusMinutes(1));
        ingestionLogRepository.save(falha);

        mockMvc.perform(get("/v1/status/freshness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fonte").value("COMEXSTAT"))
                .andExpect(jsonPath("$[0].status").value("FALHA"))
                .andExpect(jsonPath("$[0].mensagem").value("Fonte respondeu HTTP 429"))
                .andExpect(jsonPath("$[0].periodoReferencia").value("2025-01..2025-06"))
                .andExpect(jsonPath("$[0].ultimoSucessoEm").isNotEmpty());
    }

    @Test
    void semExecucoesDevolveListaVazia() throws Exception {
        mockMvc.perform(get("/v1/status/freshness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void historicoDeExecucoesEPaginadoDaMaisRecenteParaAMaisAntiga() throws Exception {
        for (int i = 0; i < 3; i++) {
            var execucao = IngestionLog.iniciar("COMEXSTAT", "EXPORT cap72 2026-0" + (i + 1), BASE.plusHours(i));
            execucao.finalizar(StatusExecucao.SUCESSO, 100 + i, 100 + i, null, BASE.plusHours(i).plusMinutes(2));
            ingestionLogRepository.save(execucao);
        }

        mockMvc.perform(get("/v1/status/executions").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                // a mais recente (BASE+2h) vem primeiro
                .andExpect(jsonPath("$.content[0].periodoReferencia").value("EXPORT cap72 2026-03"))
                .andExpect(jsonPath("$.content[0].registrosGravados").value(102));

        mockMvc.perform(get("/v1/status/executions").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].periodoReferencia").value("EXPORT cap72 2026-01"));
    }
}
