package com.steeltrade.ingestion.core;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;

import com.steeltrade.ingestion.comexstat.ComexStatIngestionService;
import com.steeltrade.ingestion.comexstat.dto.IngestionRunResponse;
import com.steeltrade.ingestion.staging.StatusProcessamento;
import com.steeltrade.shared.exception.ExternalSourceException;
import com.steeltrade.warehouse.loader.TradeFactLoader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngestionRunnerImplTest {

    private static final Instant AGORA = Instant.parse("2026-09-23T03:00:00Z");
    private static final YearMonth JANEIRO = YearMonth.of(2025, 1);
    private static final YearMonth JUNHO = YearMonth.of(2025, 6);

    @Mock
    private ComexStatIngestionService ingestionService;

    @Mock
    private TradeFactLoader tradeFactLoader;

    @Mock
    private IngestionLogRepository ingestionLogRepository;

    private IngestionRunnerImpl runner;

    @BeforeEach
    void configurar() {
        runner = new IngestionRunnerImpl(ingestionService, tradeFactLoader, ingestionLogRepository,
                Clock.fixed(AGORA, ZoneOffset.UTC));
        when(ingestionLogRepository.save(any(IngestionLog.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    private IngestionRunResponse coleta(StatusProcessamento status, String mensagem) {
        return new IngestionRunResponse(1L, status == StatusProcessamento.ERRO ? 429 : 200,
                status, mensagem, OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Test
    void pipelineCompletoRegistraExecucaoComSucessoEVolumetria() {
        when(ingestionService.coletarExportacoes(JANEIRO, JUNHO, 72))
                .thenReturn(coleta(StatusProcessamento.PENDENTE, null));
        when(tradeFactLoader.processarPendentes()).thenReturn(new TradeFactLoader.LoadResult(1, 988, 0));

        var resultado = runner.executarComexStat(JANEIRO, JUNHO, 72);

        assertThat(resultado.statusExecucao()).isEqualTo(StatusExecucao.SUCESSO);
        assertThat(resultado.carga().registrosCarregados()).isEqualTo(988);

        var captor = ArgumentCaptor.forClass(IngestionLog.class);
        verify(ingestionLogRepository, atLeastOnce()).save(captor.capture());
        var execucao = captor.getValue();
        assertThat(execucao.getFonte()).isEqualTo("COMEXSTAT");
        assertThat(execucao.getStatus()).isEqualTo(StatusExecucao.SUCESSO);
        assertThat(execucao.getPeriodoReferencia()).isEqualTo("2025-01..2025-06");
        assertThat(execucao.getRegistrosGravados()).isEqualTo(988);
        assertThat(execucao.getFinalizadoEm()).isEqualTo(OffsetDateTime.ofInstant(AGORA, ZoneOffset.UTC));
    }

    @Test
    void coletaComErroHttpRegistraFalhaSemDispararACarga() {
        when(ingestionService.coletarExportacoes(any(), any(), anyInt()))
                .thenReturn(coleta(StatusProcessamento.ERRO, "Fonte respondeu HTTP 429"));

        var resultado = runner.executarComexStat(JANEIRO, JUNHO, 72);

        assertThat(resultado.statusExecucao()).isEqualTo(StatusExecucao.FALHA);
        verify(tradeFactLoader, never()).processarPendentes();

        var captor = ArgumentCaptor.forClass(IngestionLog.class);
        verify(ingestionLogRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusExecucao.FALHA);
        assertThat(captor.getValue().getMensagem()).contains("429");
    }

    @Test
    void cargaComDeadLetterRegistraExecucaoParcial() {
        when(ingestionService.coletarExportacoes(any(), any(), anyInt()))
                .thenReturn(coleta(StatusProcessamento.PENDENTE, null));
        when(tradeFactLoader.processarPendentes()).thenReturn(new TradeFactLoader.LoadResult(1, 500, 1));

        var resultado = runner.executarComexStat(JANEIRO, JUNHO, 72);

        assertThat(resultado.statusExecucao()).isEqualTo(StatusExecucao.PARCIAL);
    }

    @Test
    void excecaoNoMeioDoPipelineRegistraFalhaEPropaga() {
        when(ingestionService.coletarExportacoes(any(), any(), anyInt()))
                .thenThrow(new ExternalSourceException("Circuito aberto para o Comex Stat", null));

        assertThatThrownBy(() -> runner.executarComexStat(JANEIRO, JUNHO, 72))
                .isInstanceOf(ExternalSourceException.class);

        var captor = ArgumentCaptor.forClass(IngestionLog.class);
        verify(ingestionLogRepository, atLeastOnce()).save(captor.capture());
        var execucao = captor.getValue();
        assertThat(execucao.getStatus()).isEqualTo(StatusExecucao.FALHA);
        assertThat(execucao.getMensagem()).contains("Circuito aberto");
        assertThat(execucao.getFinalizadoEm()).isNotNull();
    }
}
