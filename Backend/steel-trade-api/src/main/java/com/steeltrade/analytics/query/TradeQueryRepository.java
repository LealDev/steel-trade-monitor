package com.steeltrade.analytics.query;

import java.time.LocalDate;
import java.util.List;

import com.steeltrade.warehouse.fact.Fluxo;
import com.steeltrade.warehouse.fact.TradeFact;

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
}
