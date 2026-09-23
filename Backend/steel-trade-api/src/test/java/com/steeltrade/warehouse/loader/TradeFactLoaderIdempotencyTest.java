package com.steeltrade.warehouse.loader;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import com.steeltrade.TestcontainersConfiguration;
import com.steeltrade.ingestion.comexstat.ComexStatMapper;
import com.steeltrade.ingestion.comexstat.dto.RegistroComercioExterior;
import com.steeltrade.ingestion.staging.RawPayload;
import com.steeltrade.ingestion.staging.RawPayloadRepository;
import com.steeltrade.ingestion.staging.StatusProcessamento;
import com.steeltrade.warehouse.fact.TradeFactRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O teste mais importante do projeto: rodar o pipeline duas vezes com o mesmo
 * dado NÃO pode alterar os números. Sem isso, todo o dashboard vira mentira.
 * Usa o payload real da API (fixture) contra um Postgres real (Testcontainers).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TradeFactLoaderIdempotencyTest {

    @Autowired
    private RawPayloadRepository stagingRepository;

    @Autowired
    private TradeFactRepository fatoRepository;

    @Autowired
    private TradeFactLoader loader;

    @Autowired
    private ComexStatMapper mapper;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void limparBanco() {
        jdbcClient.sql("delete from fato_comercio_exterior").update();
        stagingRepository.deleteAll();
    }

    @Test
    void reprocessarOMesmoPayloadNaoDuplicaNemAlteraOsNumeros() throws IOException {
        String payload = lerFixture();

        // valores esperados com semântica de upsert: última ocorrência de cada
        // chave natural vence — exatamente o que o ON CONFLICT DO UPDATE faz
        Map<String, RegistroComercioExterior> porChaveNatural = new LinkedHashMap<>();
        for (var registro : mapper.mapear(payload)) {
            porChaveNatural.put(chaveNatural(registro), registro);
        }
        BigDecimal kgEsperado = porChaveNatural.values().stream()
                .map(RegistroComercioExterior::kgLiquido).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal fobEsperado = porChaveNatural.values().stream()
                .map(RegistroComercioExterior::valorFobUsd).reduce(BigDecimal.ZERO, BigDecimal::add);

        // ——— 1ª carga ———
        stagingRepository.save(novaStaging(payload));
        var primeiraCarga = loader.processarPendentes();

        assertThat(primeiraCarga.stagingProcessados()).isEqualTo(1);
        assertThat(primeiraCarga.registrosCarregados()).isEqualTo(988);
        assertThat(primeiraCarga.stagingComErro()).isZero();

        long linhasAposPrimeira = fatoRepository.count();
        assertThat(linhasAposPrimeira).isEqualTo(porChaveNatural.size());
        assertThat(somaFato("kg_liquido")).isEqualByComparingTo(kgEsperado);
        assertThat(somaFato("valor_fob_usd")).isEqualByComparingTo(fobEsperado);

        // ——— 2ª carga: mesmo payload chega de novo (janela móvel recarrega) ———
        stagingRepository.save(novaStaging(payload));
        var segundaCarga = loader.processarPendentes();

        assertThat(segundaCarga.stagingProcessados()).isEqualTo(1);
        assertThat(fatoRepository.count()).isEqualTo(linhasAposPrimeira);
        assertThat(somaFato("kg_liquido")).isEqualByComparingTo(kgEsperado);
        assertThat(somaFato("valor_fob_usd")).isEqualByComparingTo(fobEsperado);

        // staging inteira marcada como processada
        assertThat(stagingRepository.findAll())
                .allMatch(s -> s.getStatusProcessamento() == StatusProcessamento.PROCESSADO)
                .allMatch(s -> s.getProcessadoEm() != null);
    }

    @Test
    void payloadInvalidoViraDeadLetterSemInterromperOsDemais() throws IOException {
        stagingRepository.save(novaStaging("{\"error\":{\"code\":429}}"));
        stagingRepository.save(novaStaging(lerFixture()));

        var resultado = loader.processarPendentes();

        assertThat(resultado.stagingComErro()).isEqualTo(1);
        assertThat(resultado.stagingProcessados()).isEqualTo(1);
        assertThat(stagingRepository.findAll())
                .anyMatch(s -> s.getStatusProcessamento() == StatusProcessamento.ERRO
                        && s.getMensagemErro() != null);
    }

    private RawPayload novaStaging(String payload) {
        return new RawPayload("COMEXSTAT", "/general?language=pt", "{}", payload,
                200, StatusProcessamento.PENDENTE, null, OffsetDateTime.now());
    }

    private String chaveNatural(RegistroComercioExterior r) {
        return String.join("|", "EXPORT", String.valueOf(r.ano()), String.valueOf(r.mes()),
                r.codigoNcm(), r.nomePais(), r.nomeUf(), r.descricaoVia());
    }

    private BigDecimal somaFato(String coluna) {
        return jdbcClient.sql("select coalesce(sum(" + coluna + "), 0) from fato_comercio_exterior")
                .query(BigDecimal.class).single();
    }

    private String lerFixture() throws IOException {
        try (var stream = getClass().getResourceAsStream("/fixtures/comexstat-general-export-cap72-2025-06.json")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
