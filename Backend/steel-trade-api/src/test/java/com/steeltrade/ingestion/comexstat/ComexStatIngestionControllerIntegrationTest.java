package com.steeltrade.ingestion.comexstat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.steeltrade.TestcontainersConfiguration;
import com.steeltrade.ingestion.comexstat.dto.ComexStatRawResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de rota: o contrato REST de ponta a ponta contra banco real,
 * com a fonte externa substituída na porta (o gateway é mockado — nenhum
 * teste gasta cota da API).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ComexStatIngestionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ComexStatGateway gateway;

    @Test
    void pipelineCompletoPelaRota_coletaCargaEConsulta() throws Exception {
        when(gateway.buscarExportacoes(any(), any(), anyInt())).thenReturn(new ComexStatRawResponse(
                "/general?language=pt", "{\"flow\":\"export\"}", 200, lerFixture()));

        mockMvc.perform(post("/v1/ingestion/comexstat/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"2025-06\",\"to\":\"2025-06\",\"chapter\":72}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.execucaoId").isNumber())
                .andExpect(jsonPath("$.statusExecucao").value("SUCESSO"))
                .andExpect(jsonPath("$.coleta.stagingId").isNumber())
                .andExpect(jsonPath("$.coleta.statusHttp").value(200))
                .andExpect(jsonPath("$.coleta.status").value("PENDENTE"))
                .andExpect(jsonPath("$.carga.stagingProcessados").value(1))
                .andExpect(jsonPath("$.carga.registrosCarregados").value(988))
                .andExpect(jsonPath("$.carga.stagingComErro").value(0));

        mockMvc.perform(get("/v1/trade/time-series")
                        .param("flow", "EXPORT")
                        .param("ncmChapter", "72")
                        .param("from", "2025-06")
                        .param("to", "2025-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].period").value("2025-06"))
                .andExpect(jsonPath("$[0].kgLiquido").isNumber())
                .andExpect(jsonPath("$[0].valorFobUsd").isNumber());
    }

    @Test
    void rotaDeConsultaSemDadosDevolveListaVazia() throws Exception {
        mockMvc.perform(get("/v1/trade/time-series")
                        .param("from", "1999-01")
                        .param("to", "1999-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rotaDeIngestaoValidaOPeriodo() throws Exception {
        mockMvc.perform(post("/v1/ingestion/comexstat/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"2025-06\",\"to\":\"2025-01\",\"chapter\":72}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void fonteExternaRespondendoErroDevolve502ComDeadLetterRegistrada() throws Exception {
        when(gateway.buscarExportacoes(any(), any(), anyInt())).thenReturn(new ComexStatRawResponse(
                "/general?language=pt", "{}", 429, "{\"error\":{\"code\":429}}"));

        mockMvc.perform(post("/v1/ingestion/comexstat/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"2025-07\",\"to\":\"2025-07\",\"chapter\":72}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.statusExecucao").value("FALHA"))
                .andExpect(jsonPath("$.coleta.status").value("ERRO"))
                .andExpect(jsonPath("$.coleta.mensagemErro").value("Fonte respondeu HTTP 429"))
                .andExpect(jsonPath("$.carga.registrosCarregados").value(0));
    }

    private String lerFixture() throws IOException {
        try (var stream = getClass().getResourceAsStream("/fixtures/comexstat-general-export-cap72-2025-06.json")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
