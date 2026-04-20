package com.rutasproyect.damii.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rutasproyect.damii.model.RouteStop;

import java.util.List;

@Repository
public interface RouteStopRepository extends JpaRepository<RouteStop, Integer> {
    @org.springframework.data.jpa.repository.Query("SELECT rs FROM RouteStop rs JOIN FETCH rs.stop WHERE rs.route.id = :routeId")
    List<RouteStop> findByRouteIdWithStops(@org.springframework.data.repository.query.Param("routeId") Integer routeId);
}
