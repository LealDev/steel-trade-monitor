package com.steeltrade.ingestion.staging;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Payload cru como veio da fonte externa (tabela stg_coleta_bruta).
 * Zona de pouso do ELT: sem regra de negócio, sem transformação.
 */
@Entity
@Table(name = "stg_coleta_bruta")
public class RawPayload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fonte;

    @Column(nullable = false)
    private String endpoint;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String parametros;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Column(name = "status_http", nullable = false)
    private Integer statusHttp;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_processamento", nullable = false)
    private StatusProcessamento statusProcessamento;

    @Column(name = "mensagem_erro")
    private String mensagemErro;

    @Column(name = "coletado_em", nullable = false)
    private OffsetDateTime coletadoEm;

    @Column(name = "processado_em")
    private OffsetDateTime processadoEm;

    protected RawPayload() {
        // exigido pelo JPA
    }

    public RawPayload(String fonte, String endpoint, String parametros, String payload,
                      Integer statusHttp, StatusProcessamento statusProcessamento,
                      String mensagemErro, OffsetDateTime coletadoEm) {
        this.fonte = fonte;
        this.endpoint = endpoint;
        this.parametros = parametros;
        this.payload = payload;
        this.statusHttp = statusHttp;
        this.statusProcessamento = statusProcessamento;
        this.mensagemErro = mensagemErro;
        this.coletadoEm = coletadoEm;
    }

    public void marcarProcessado(OffsetDateTime quando) {
        this.statusProcessamento = StatusProcessamento.PROCESSADO;
        this.processadoEm = quando;
        this.mensagemErro = null;
    }

    public void marcarErro(String mensagem, OffsetDateTime quando) {
        this.statusProcessamento = StatusProcessamento.ERRO;
        this.processadoEm = quando;
        this.mensagemErro = mensagem;
    }

    public Long getId() {
        return id;
    }

    public String getFonte() {
        return fonte;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getParametros() {
        return parametros;
    }

    public String getPayload() {
        return payload;
    }

    public Integer getStatusHttp() {
        return statusHttp;
    }

    public StatusProcessamento getStatusProcessamento() {
        return statusProcessamento;
    }

    public String getMensagemErro() {
        return mensagemErro;
    }

    public OffsetDateTime getColetadoEm() {
        return coletadoEm;
    }

    public OffsetDateTime getProcessadoEm() {
        return processadoEm;
    }
}
