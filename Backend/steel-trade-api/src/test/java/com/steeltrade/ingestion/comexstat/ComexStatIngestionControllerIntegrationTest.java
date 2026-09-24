package com.steeltrade.ingestion.comexstat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.steeltrade.TestcontainersConfiguration;
import com.steeltrade.ingestion.comexstat.dto.ComexStatRawResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
@SpringBootTest(properties = "comexstat.pausa-entre-meses-ms=0")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ComexStatIngestionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ComexStatGateway gateway;

    @Test
    void pipelineCompletoPelaRota_coletaCargaEConsultas() throws Exception {
        when(gateway.buscar(any(), any(), any(), anyInt())).thenReturn(new ComexStatRawResponse(
                "/general?language=pt", "{\"flow\":\"export\"}", 200, lerFixture()));

        mockMvc.perform(post("/v1/ingestion/comexstat/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"2025-06\",\"to\":\"2025-06\",\"chapter\":72}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.execucaoId").isNumber())
                .andExpect(jsonPath("$.statusExecucao").value("SUCESSO"))
                .andExpect(jsonPath("$.mesesColetados").value(1))
                .andExpect(jsonPath("$.mesesComErro").value(0))
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

        // ranking de países: na fixture real, Estados Unidos lidera por FOB
        mockMvc.perform(get("/v1/trade/top-countries")
                        .param("from", "2025-06")
                        .param("to", "2025-06")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].nomePais").value("Estados Unidos"))
                .andExpect(jsonPath("$[0].valorFobUsd").isNumber());

        // recorte por via: MARITIMA domina o volume na fixture real
        mockMvc.perform(get("/v1/trade/by-transport")
                        .param("from", "2025-06")
                        .param("to", "2025-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].via").value("MARITIMA"))
                .andExpect(jsonPath("$[0].kgLiquido").isNumber());
    }

    @Test
    void coletaDeImportacaoRegistraFluxoImportNoFato() throws Exception {
        when(gateway.buscar(any(), any(), any(), anyInt())).thenReturn(new ComexStatRawResponse(
                "/general?language=pt", "{\"flow\":\"import\"}", 200,
                "{\"data\":{\"list\":[{\"coNcm\":\"73181500\",\"year\":\"2025\",\"monthNumber\":\"03\","
                        + "\"country\":\"China\",\"state\":\"São Paulo\",\"via\":\"MARITIMA\","
                        + "\"ncm\":\"Parafusos\",\"metricFOB\":\"1000\",\"metricKG\":\"500\"}]}}"));

        mockMvc.perform(post("/v1/ingestion/comexstat/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"2025-03\",\"to\":\"2025-03\",\"chapter\":73,\"flow\":\"IMPORT\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusExecucao").value("SUCESSO"))
                .andExpect(jsonPath("$.carga.registrosCarregados").value(1));

        mockMvc.perform(get("/v1/trade/time-series")
                        .param("flow", "IMPORT")
                        .param("ncmChapter", "73")
                        .param("from", "2025-03")
                        .param("to", "2025-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].kgLiquido").value(500));
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
        when(gateway.buscar(any(), any(), any(), anyInt())).thenReturn(new ComexStatRawResponse(
                "/general?language=pt", "{\"flow\":\"export\"}", 429, "{\"error\":{\"code\":429}}"));

        mockMvc.perform(post("/v1/ingestion/comexstat/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"2025-07\",\"to\":\"2025-07\",\"chapter\":72}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.statusExecucao").value("FALHA"))
                .andExpect(jsonPath("$.mesesColetados").value(0))
                .andExpect(jsonPath("$.mesesComErro").value(1))
                .andExpect(jsonPath("$.carga.registrosCarregados").value(0));
    }

    private String lerFixture() throws IOException {
        try (var stream = getClass().getResourceAsStream("/fixtures/comexstat-general-export-cap72-2025-06.json")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
