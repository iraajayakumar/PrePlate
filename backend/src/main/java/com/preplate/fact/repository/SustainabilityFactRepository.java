package com.preplate.fact.repository;

import com.preplate.fact.entity.SustainabilityFact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SustainabilityFactRepository extends JpaRepository<SustainabilityFact, Long> {
}
