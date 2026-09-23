package com.steeltrade.ingestion.comexstat;

import java.nio.charset.StandardCharsets;
import java.time.YearMonth;

import com.steeltrade.ingestion.comexstat.dto.ComexStatQueryRequest;
import com.steeltrade.ingestion.comexstat.dto.ComexStatRawResponse;
import com.steeltrade.shared.exception.ExternalSourceException;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.ObjectMapper;

/**
 * Adaptador HTTP da porta {@link ComexStatGateway}.
 * Devolve a resposta crua inclusive em status de erro (429, 500...):
 * quem decide o que fazer com ela é o service, não o transporte.
 */
@Component
public class ComexStatHttpClient implements ComexStatGateway {

    static final String ENDPOINT_GENERAL = "/general?language=pt";

    private final RestClient comexStatRestClient;
    private final ObjectMapper objectMapper;

    public ComexStatHttpClient(RestClient comexStatRestClient, ObjectMapper objectMapper) {
        this.comexStatRestClient = comexStatRestClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public ComexStatRawResponse buscarExportacoes(YearMonth de, YearMonth ate, int capitulo) {
        var consulta = ComexStatQueryRequest.exportacoesPorCapitulo(de, ate, capitulo);
        String parametros = objectMapper.writeValueAsString(consulta);
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
