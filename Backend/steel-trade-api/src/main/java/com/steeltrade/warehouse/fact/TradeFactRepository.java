package com.steeltrade.warehouse.fact;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeFactRepository extends JpaRepository<TradeFact, Long> {
}
