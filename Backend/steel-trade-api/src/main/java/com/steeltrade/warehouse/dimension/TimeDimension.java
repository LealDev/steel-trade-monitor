package com.steeltrade.warehouse.dimension;

import java.time.LocalDate;
import java.time.YearMonth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "dim_tempo")
public class TimeDimension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer ano;

    @Column(nullable = false)
    private Integer mes;

    @Column(nullable = false)
    private Integer trimestre;

    @Column(name = "ano_mes", nullable = false)
    private String anoMes;

    @Column(name = "primeiro_dia", nullable = false)
    private LocalDate primeiroDia;

    protected TimeDimension() {
    }

    public static TimeDimension de(int ano, int mes) {
        var dimensao = new TimeDimension();
        dimensao.ano = ano;
        dimensao.mes = mes;
        dimensao.trimestre = ((mes - 1) / 3) + 1;
        dimensao.anoMes = YearMonth.of(ano, mes).toString();
        dimensao.primeiroDia = LocalDate.of(ano, mes, 1);
        return dimensao;
    }

    public Long getId() {
        return id;
    }

    public Integer getAno() {
        return ano;
    }

    public Integer getMes() {
        return mes;
    }

    public Integer getTrimestre() {
        return trimestre;
    }

    public String getAnoMes() {
        return anoMes;
    }

    public LocalDate getPrimeiroDia() {
        return primeiroDia;
    }
}
