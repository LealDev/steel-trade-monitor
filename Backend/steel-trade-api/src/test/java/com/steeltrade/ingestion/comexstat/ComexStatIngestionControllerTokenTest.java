package com.steeltrade.ingestion.comexstat;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.steeltrade.ingestion.comexstat.dto.IngestionRunResponse;
import com.steeltrade.ingestion.comexstat.dto.PipelineRunResponse;
import com.steeltrade.ingestion.core.IngestionRunner;
import com.steeltrade.ingestion.core.StatusExecucao;
import com.steeltrade.ingestion.staging.StatusProcessamento;
import com.steeltrade.warehouse.loader.TradeFactLoader;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rota de ingestão protegida por token quando app.ingestion.token está
 * definido (cenário de produção).
 */
@WebMvcTest(controllers = ComexStatIngestionController.class,
        properties = "app.ingestion.token=segredo-de-teste")
@Import(ComexStatIngestionControllerTokenTest.ClockDeTeste.class)
class ComexStatIngestionControllerTokenTest {

    private static final String BODY = "{\"from\":\"2025-06\",\"to\":\"2025-06\",\"chapter\":72}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IngestionRunner ingestionRunner;

    @Test
    void semTokenDevolve401SemExecutarNada() throws Exception {
        mockMvc.perform(post("/v1/ingestion/comexstat/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized());

        verify(ingestionRunner, never()).executarComexStat(any(), any(), anyInt());
    }

    @Test
    void tokenErradoDevolve401() throws Exception {
        mockMvc.perform(post("/v1/ingestion/comexstat/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ComexStatIngestionController.HEADER_TOKEN, "chute")
                        .content(BODY))
                .andExpect(status().isUnauthorized());

        verify(ingestionRunner, never()).executarComexStat(any(), any(), anyInt());
    }

    @Test
    void tokenCorretoExecutaOPipeline() throws Exception {
        when(ingestionRunner.executarComexStat(any(), any(), anyInt())).thenReturn(new PipelineRunResponse(
                1L, StatusExecucao.SUCESSO,
                new IngestionRunResponse(1L, 200, StatusProcessamento.PENDENTE, null,
                        OffsetDateTime.now(ZoneOffset.UTC)),
                new TradeFactLoader.LoadResult(1, 988, 0)));

        mockMvc.perform(post("/v1/ingestion/comexstat/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ComexStatIngestionController.HEADER_TOKEN, "segredo-de-teste")
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusExecucao").value("SUCESSO"));
    }

    @TestConfiguration
    static class ClockDeTeste {
        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }
}
