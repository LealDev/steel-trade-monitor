package com.steeltrade.warehouse.dimension.repository;

import java.util.Optional;

import com.steeltrade.warehouse.dimension.TimeDimension;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeDimensionRepository extends JpaRepository<TimeDimension, Long> {

    Optional<TimeDimension> findByAnoAndMes(Integer ano, Integer mes);
}
