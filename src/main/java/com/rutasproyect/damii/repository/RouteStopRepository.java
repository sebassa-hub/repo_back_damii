package com.rutasproyect.damii.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rutasproyect.damii.model.RouteStop;

@Repository
public interface RouteStopRepository extends JpaRepository<RouteStop, Integer> {
}
