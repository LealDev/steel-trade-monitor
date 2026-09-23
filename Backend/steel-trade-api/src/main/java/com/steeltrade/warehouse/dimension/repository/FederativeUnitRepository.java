package com.steeltrade.warehouse.dimension.repository;

import java.util.Optional;

import com.steeltrade.warehouse.dimension.FederativeUnit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FederativeUnitRepository extends JpaRepository<FederativeUnit, Long> {

    Optional<FederativeUnit> findByNome(String nome);
}
