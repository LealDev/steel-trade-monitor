package com.steeltrade.ingestion.comexstat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.steeltrade.ingestion.comexstat.dto.RegistroComercioExterior;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Camada anticorrupção: traduz o payload cru do Comex Stat para o modelo
 * interno. Se o MDIC renomear um campo, conserta-se este arquivo — só ele.
 */
@Component
public class ComexStatMapper {

    private final ObjectMapper objectMapper;

    public ComexStatMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<RegistroComercioExterior> mapear(String payloadJson) {
        JsonNode raiz = objectMapper.readTree(payloadJson);
        JsonNode lista = raiz.path("data").path("list");
        if (!lista.isArray()) {
            throw new IllegalArgumentException("Payload sem o array data.list — formato inesperado do Comex Stat");
        }
        List<RegistroComercioExterior> registros = new ArrayList<>(lista.size());
        for (JsonNode linha : lista) {
            registros.add(new RegistroComercioExterior(
                    textoObrigatorio(linha, "coNcm"),
                    textoObrigatorio(linha, "ncm"),
                    Integer.parseInt(textoObrigatorio(linha, "year")),
                    Integer.parseInt(textoObrigatorio(linha, "monthNumber")),
                    textoObrigatorio(linha, "country"),
                    textoObrigatorio(linha, "state"),
                    textoObrigatorio(linha, "via"),
                    new BigDecimal(textoObrigatorio(linha, "metricKG")),
                    new BigDecimal(textoObrigatorio(linha, "metricFOB"))));
        }
        return registros;
    }

    private String textoObrigatorio(JsonNode linha, String campo) {
        JsonNode valor = linha.path(campo);
        if (valor.isMissingNode() || valor.isNull()) {
            throw new IllegalArgumentException("Campo obrigatório ausente no payload: " + campo);
        }
        return valor.asString();
    }
}
