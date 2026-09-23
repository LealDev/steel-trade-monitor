package com.steeltrade.warehouse.dimension.repository;

import java.util.Optional;

import com.steeltrade.warehouse.dimension.Country;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryRepository extends JpaRepository<Country, Long> {

    Optional<Country> findByNomePt(String nomePt);
}
