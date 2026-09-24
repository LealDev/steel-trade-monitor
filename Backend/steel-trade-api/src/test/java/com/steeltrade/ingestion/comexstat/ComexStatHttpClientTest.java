package com.steeltrade.ingestion.comexstat;

import java.time.Duration;
import java.time.YearMonth;

import com.steeltrade.ingestion.comexstat.dto.ComexStatRawResponse;
import com.steeltrade.shared.config.ComexStatProperties;
import com.steeltrade.shared.config.HttpClientConfig;
import com.steeltrade.shared.exception.ExternalSourceException;
import com.steeltrade.warehouse.fact.Fluxo;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
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
 * Contrato do adaptador HTTP: formato do body, headers e comportamento de
 * retry/circuit breaker — o formato foi validado contra a API real em 2026-09.
 * As proteções usam os mesmos predicados da produção, com esperas de 1ms.
 */
class ComexStatHttpClientTest {

    private static final String BASE_URL = "https://comexstat.teste";
    private static final String URL_GENERAL = BASE_URL + "/general?language=pt";
    private static final YearMonth JUNHO = YearMonth.of(2025, 6);

    private MockRestServiceServer servidor;
    private RestClient restClient;

    @BeforeEach
    void configurar() {
        RestClient.Builder builder = RestClient.builder();
        var properties = new ComexStatProperties(BASE_URL, "UA-Teste/1.0", 6, 0);
        // aplica a config real (baseUrl, User-Agent) e só depois troca o
        // requestFactory pelo mock — a ordem inversa perderia o mock
        new HttpClientConfig().comexStatRestClient(builder, properties);
        servidor = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
    }

    private ComexStatHttpClient criarClient(int tentativas, CircuitBreaker circuitBreaker) {
        var retryConfig = RetryConfig.<ComexStatRawResponse>custom()
                .maxAttempts(tentativas)
                .waitDuration(Duration.ofMillis(1))
                .retryOnException(ex -> ex instanceof ExternalSourceException)
                .retryOnResult(r -> r.statusHttp() == 429 || r.statusHttp() >= 500)
                .build();
        return new ComexStatHttpClient(restClient, JsonMapper.builder().build(),
                Retry.of("teste", retryConfig), circuitBreaker);
    }

    private CircuitBreaker circuitoPermissivo() {
        return CircuitBreaker.ofDefaults("teste-permissivo");
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

        var client = criarClient(1, circuitoPermissivo());
        var resposta = client.buscar(Fluxo.EXPORT, YearMonth.of(2025, 1), JUNHO, 72);

        assertThat(resposta.sucesso()).isTrue();
        assertThat(resposta.statusHttp()).isEqualTo(200);
        assertThat(resposta.corpo()).isEqualTo("{\"data\":{\"list\":[]}}");
        assertThat(resposta.endpoint()).isEqualTo("/general?language=pt");
        assertThat(resposta.parametrosJson()).contains("\"chapter\"");
        servidor.verify();
    }

    @Test
    void fluxoDeImportacaoUsaFlowImportNoBody() {
        servidor.expect(requestTo(URL_GENERAL))
                .andExpect(jsonPath("$.flow").value("import"))
                .andExpect(jsonPath("$.filters[0].values[0]").value(73))
                .andRespond(withSuccess("{\"data\":{\"list\":[]}}", MediaType.APPLICATION_JSON));

        var client = criarClient(1, circuitoPermissivo());
        var resposta = client.buscar(Fluxo.IMPORT, JUNHO, JUNHO, 73);

        assertThat(resposta.sucesso()).isTrue();
        servidor.verify();
    }

    @Test
    void tentaDeNovoApos429EDevolveOSucesso() {
        servidor.expect(requestTo(URL_GENERAL))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"code\":429}}"));
        servidor.expect(requestTo(URL_GENERAL))
                .andRespond(withSuccess("{\"data\":{\"list\":[]}}", MediaType.APPLICATION_JSON));

        var client = criarClient(3, circuitoPermissivo());
        var resposta = client.buscar(Fluxo.EXPORT, JUNHO, JUNHO, 72);

        assertThat(resposta.statusHttp()).isEqualTo(200);
        servidor.verify();
    }

    @Test
    void esgotadasAsTentativasDevolveAUltimaRespostaDeErro() {
        servidor.expect(ExpectedCount.times(3), requestTo(URL_GENERAL))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"code\":429}}"));

        var client = criarClient(3, circuitoPermissivo());
        var resposta = client.buscar(Fluxo.EXPORT, JUNHO, JUNHO, 72);

        assertThat(resposta.sucesso()).isFalse();
        assertThat(resposta.statusHttp()).isEqualTo(429);
        servidor.verify();
    }

    @Test
    void traduzFalhaDeRedeParaExternalSourceException() {
        servidor.expect(ExpectedCount.times(2), requestTo(URL_GENERAL))
                .andRespond(withException(new java.io.IOException("conexão recusada")));

        var client = criarClient(2, circuitoPermissivo());

        assertThatThrownBy(() -> client.buscar(Fluxo.EXPORT, JUNHO, JUNHO, 72))
                .isInstanceOf(ExternalSourceException.class)
                .hasMessageContaining("Comex Stat");
        servidor.verify();
    }

    @Test
    void circuitoAbertoFalhaRapidoSemChamarARede() {
        // circuito sensível: uma única falha de rede abre
        var circuitBreaker = CircuitBreaker.of("teste-sensivel", CircuitBreakerConfig.custom()
                .slidingWindowSize(1)
                .minimumNumberOfCalls(1)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .recordException(ex -> ex instanceof ExternalSourceException)
                .build());
        servidor.expect(requestTo(URL_GENERAL))
                .andRespond(withException(new java.io.IOException("conexão recusada")));

        var client = criarClient(1, circuitBreaker);

        assertThatThrownBy(() -> client.buscar(Fluxo.EXPORT, JUNHO, JUNHO, 72))
                .isInstanceOf(ExternalSourceException.class);
        // segunda chamada: o circuito está aberto, a rede nem é tentada
        assertThatThrownBy(() -> client.buscar(Fluxo.EXPORT, JUNHO, JUNHO, 72))
                .isInstanceOf(ExternalSourceException.class)
                .hasMessageContaining("Circuito aberto");
        servidor.verify();
    }
}
