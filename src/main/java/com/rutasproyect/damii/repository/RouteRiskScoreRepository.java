package com.rutasproyect.damii.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rutasproyect.damii.model.RouteRiskScore;

@Repository
public interface RouteRiskScoreRepository extends JpaRepository<RouteRiskScore, Integer> {
}
