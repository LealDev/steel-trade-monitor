package com.steeltrade.ingestion.comexstat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A camada anticorrupção testada contra o payload REAL da API
 * (fixture coletada em 2026-09) — não contra um payload imaginado.
 */
class ComexStatMapperTest {

    private final ComexStatMapper mapper = new ComexStatMapper(JsonMapper.builder().build());

    @Test
    void traduzPayloadRealCompletoParaOModeloInterno() throws IOException {
        var registros = mapper.mapear(lerFixture());

        assertThat(registros).hasSize(988);

        var primeiro = registros.getFirst();
        assertThat(primeiro.codigoNcm()).isEqualTo("72071200");
        assertThat(primeiro.ano()).isEqualTo(2025);
        assertThat(primeiro.mes()).isEqualTo(6);
        assertThat(primeiro.nomePais()).isEqualTo("Estados Unidos");
        assertThat(primeiro.nomeUf()).isEqualTo("Rio de Janeiro");
        assertThat(primeiro.descricaoVia()).isEqualTo("MARITIMA");
        assertThat(primeiro.kgLiquido()).isEqualByComparingTo(new BigDecimal("275787105"));
        assertThat(primeiro.valorFobUsd()).isEqualByComparingTo(new BigDecimal("148932332"));

        // campos derivados do código NCM
        assertThat(primeiro.capitulo()).isEqualTo(72);
        assertThat(primeiro.sh4()).isEqualTo("7207");
        assertThat(primeiro.sh6()).isEqualTo("720712");

        // linhas com métricas zeradas são legítimas e devem ser mantidas
        assertThat(registros).anyMatch(r -> r.kgLiquido().signum() == 0);
    }

    @Test
    void rejeitaPayloadSemDataList() {
        assertThatThrownBy(() -> mapper.mapear("{\"error\":{\"code\":429}}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("data.list");
    }

    @Test
    void rejeitaLinhaSemCampoObrigatorio() {
        String payload = "{\"data\":{\"list\":[{\"coNcm\":\"72011000\"}]}}";
        assertThatThrownBy(() -> mapper.mapear(payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obrigatório");
    }

    private String lerFixture() throws IOException {
        try (var stream = getClass().getResourceAsStream("/fixtures/comexstat-general-export-cap72-2025-06.json")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
