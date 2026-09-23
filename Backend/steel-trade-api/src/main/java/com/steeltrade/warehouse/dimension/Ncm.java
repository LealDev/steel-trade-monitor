package com.steeltrade.warehouse.dimension;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "dim_ncm")
public class Ncm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_ncm", nullable = false)
    private String codigoNcm;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false)
    private String sh4;

    @Column(nullable = false)
    private String sh6;

    @Column(nullable = false)
    private Integer capitulo;

    protected Ncm() {
    }

    public Ncm(String codigoNcm, String descricao, String sh4, String sh6, Integer capitulo) {
        this.codigoNcm = codigoNcm;
        this.descricao = descricao;
        this.sh4 = sh4;
        this.sh6 = sh6;
        this.capitulo = capitulo;
    }

    public Long getId() {
        return id;
    }

    public String getCodigoNcm() {
        return codigoNcm;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getSh4() {
        return sh4;
    }

    public String getSh6() {
        return sh6;
    }

    public Integer getCapitulo() {
        return capitulo;
    }
}
