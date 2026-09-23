package com.steeltrade.ingestion.comexstat;

import java.time.YearMonth;

import com.steeltrade.shared.config.ComexStatProperties;
import com.steeltrade.shared.config.HttpClientConfig;
import com.steeltrade.shared.exception.ExternalSourceException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Contrato do adaptador HTTP: formato do body, headers e comportamento
 * diante de erro — validados contra a API real em 2026-09.
 */
class ComexStatHttpClientTest {

    private static final String BASE_URL = "https://comexstat.teste";
    private static final String URL_GENERAL = BASE_URL + "/general?language=pt";

    private MockRestServiceServer servidor;
    private ComexStatHttpClient client;

    @BeforeEach
    void configurar() {
        RestClient.Builder builder = RestClient.builder();
        var properties = new ComexStatProperties(BASE_URL, "UA-Teste/1.0", 6);
        // aplica a config real (baseUrl, User-Agent) e só depois troca o
        // requestFactory pelo mock — a ordem inversa perderia o mock
        new HttpClientConfig().comexStatRestClient(builder, properties);
        servidor = MockRestServiceServer.bindTo(builder).build();
        client = new ComexStatHttpClient(builder.build(), JsonMapper.builder().build());
    }

    @Test
    void montaRequisicaoNoFormatoDoContratoComexStat() {
        servidor.expect(requestTo(URL_GENERAL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.USER_AGENT, "UA-Teste/1.0"))
                .andExpect(jsonPath("$.flow").value("export"))
                .andExpect(jsonPath("$.monthDetail").value(true))
                .andExpect(jsonPath("$.period.from").value("2025-01"))
                .andExpect(jsonPath("$.period.to").value("2025-06"))
                .andExpect(jsonPath("$.filters[0].filter").value("chapter"))
                .andExpect(jsonPath("$.filters[0].values[0]").value(72))
                .andExpect(jsonPath("$.details[0]").value("country"))
                .andExpect(jsonPath("$.details[2]").value("via"))
                .andExpect(jsonPath("$.metrics[0]").value("metricFOB"))
                .andRespond(withSuccess("{\"data\":{\"list\":[]}}", MediaType.APPLICATION_JSON));

        var resposta = client.buscarExportacoes(YearMonth.of(2025, 1), YearMonth.of(2025, 6), 72);

        assertThat(resposta.sucesso()).isTrue();
        assertThat(resposta.statusHttp()).isEqualTo(200);
        assertThat(resposta.corpo()).isEqualTo("{\"data\":{\"list\":[]}}");
        assertThat(resposta.endpoint()).isEqualTo("/general?language=pt");
        assertThat(resposta.parametrosJson()).contains("\"chapter\"");
        servidor.verify();
    }

    @Test
    void devolveRespostaDeErroInteiraSemLancarExcecao() {
        servidor.expect(requestTo(URL_GENERAL))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"code\":429}}"));

        var resposta = client.buscarExportacoes(YearMonth.of(2025, 6), YearMonth.of(2025, 6), 72);

        assertThat(resposta.sucesso()).isFalse();
        assertThat(resposta.statusHttp()).isEqualTo(429);
        assertThat(resposta.corpo()).contains("429");
    }

    @Test
    void traduzFalhaDeRedeParaExternalSourceException() {
        servidor.expect(requestTo(URL_GENERAL))
                .andRespond(withException(new java.io.IOException("conexão recusada")));

        assertThatThrownBy(() -> client.buscarExportacoes(YearMonth.of(2025, 6), YearMonth.of(2025, 6), 72))
                .isInstanceOf(ExternalSourceException.class)
                .hasMessageContaining("Comex Stat");
    }
}
