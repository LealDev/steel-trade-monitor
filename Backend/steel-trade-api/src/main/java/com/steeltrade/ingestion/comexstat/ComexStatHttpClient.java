package com.steeltrade.ingestion.comexstat;

import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.function.Supplier;

import com.steeltrade.ingestion.comexstat.dto.ComexStatQueryRequest;
import com.steeltrade.ingestion.comexstat.dto.ComexStatRawResponse;
import com.steeltrade.shared.exception.ExternalSourceException;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.ObjectMapper;

/**
 * Adaptador HTTP da porta {@link ComexStatGateway}, protegido por retry com
 * backoff exponencial e circuit breaker (ver {@code ResilienceConfig}).
 * Devolve a resposta crua inclusive em status de erro (429, 500...):
 * quem decide o que fazer com ela é o service, não o transporte.
 */
@Component
public class ComexStatHttpClient implements ComexStatGateway {

    static final String ENDPOINT_GENERAL = "/general?language=pt";

    private final RestClient comexStatRestClient;
    private final ObjectMapper objectMapper;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;

    public ComexStatHttpClient(RestClient comexStatRestClient,
                               ObjectMapper objectMapper,
                               Retry comexStatRetry,
                               CircuitBreaker comexStatCircuitBreaker) {
        this.comexStatRestClient = comexStatRestClient;
        this.objectMapper = objectMapper;
        this.retry = comexStatRetry;
        this.circuitBreaker = comexStatCircuitBreaker;
    }

    @Override
    public ComexStatRawResponse buscarExportacoes(YearMonth de, YearMonth ate, int capitulo) {
        var consulta = ComexStatQueryRequest.exportacoesPorCapitulo(de, ate, capitulo);
        String parametros = objectMapper.writeValueAsString(consulta);

        Supplier<ComexStatRawResponse> chamada = () -> executarPost(consulta, parametros);
        Supplier<ComexStatRawResponse> protegida =
                Retry.decorateSupplier(retry, CircuitBreaker.decorateSupplier(circuitBreaker, chamada));
        try {
            return protegida.get();
        } catch (CallNotPermittedException ex) {
            throw new ExternalSourceException(
                    "Circuito aberto para o Comex Stat: fonte instável, aguardando janela de recuperação", ex);
        }
    }

    private ComexStatRawResponse executarPost(ComexStatQueryRequest consulta, String parametros) {
        try {
            return comexStatRestClient.post()
                    .uri(ENDPOINT_GENERAL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(consulta)
                    .exchange((request, response) -> {
                        String corpo = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
                        return new ComexStatRawResponse(ENDPOINT_GENERAL, parametros,
                                response.getStatusCode().value(), corpo);
                    });
        } catch (ResourceAccessException ex) {
            throw new ExternalSourceException("Falha de comunicação com o Comex Stat: " + ex.getMessage(), ex);
        }
    }
}
