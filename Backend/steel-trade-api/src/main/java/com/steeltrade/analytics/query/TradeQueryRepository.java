package com.steeltrade.analytics.query;

import java.time.LocalDate;
import java.util.List;

import com.steeltrade.warehouse.fact.Fluxo;
import com.steeltrade.warehouse.fact.TradeFact;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Consultas analíticas sobre o fato. A agregação acontece no banco
 * (GROUP BY), nunca na aplicação.
 */
public interface TradeQueryRepository extends Repository<TradeFact, Long> {

    @Query("""
            select t.anoMes as period,
                   sum(f.kgLiquido) as kgLiquido,
                   sum(f.valorFobUsd) as valorFobUsd
            from TradeFact f
                join f.tempo t
                join f.ncm n
            where f.fluxo = :fluxo
              and n.capitulo = :capitulo
              and t.primeiroDia between :de and :ate
            group by t.anoMes
            order by t.anoMes
            """)
    List<TimeSeriesPointView> serieTemporal(@Param("fluxo") Fluxo fluxo,
                                            @Param("capitulo") int capitulo,
                                            @Param("de") LocalDate de,
                                            @Param("ate") LocalDate ate);

    @Query("""
            select p.nomePt as nomePais,
                   sum(f.kgLiquido) as kgLiquido,
                   sum(f.valorFobUsd) as valorFobUsd
            from TradeFact f
                join f.pais p
                join f.tempo t
                join f.ncm n
            where f.fluxo = :fluxo
              and n.capitulo = :capitulo
              and t.primeiroDia between :de and :ate
            group by p.nomePt
            order by sum(f.valorFobUsd) desc
            """)
    List<CountryRankingView> rankingDePaises(@Param("fluxo") Fluxo fluxo,
                                             @Param("capitulo") int capitulo,
                                             @Param("de") LocalDate de,
                                             @Param("ate") LocalDate ate,
                                             Pageable pageable);

    @Query("""
            select v.descricao as via,
                   sum(f.kgLiquido) as kgLiquido,
                   sum(f.valorFobUsd) as valorFobUsd
            from TradeFact f
                join f.via v
                join f.tempo t
                join f.ncm n
            where f.fluxo = :fluxo
              and n.capitulo = :capitulo
              and t.primeiroDia between :de and :ate
            group by v.descricao
            order by sum(f.kgLiquido) desc
            """)
    List<TransportBreakdownView> recortePorVia(@Param("fluxo") Fluxo fluxo,
                                               @Param("capitulo") int capitulo,
                                               @Param("de") LocalDate de,
                                               @Param("ate") LocalDate ate);
}
