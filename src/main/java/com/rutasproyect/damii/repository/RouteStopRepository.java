package com.rutasproyect.damii.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.rutasproyect.damii.model.RouteStop;

import java.util.List;

@Repository
public interface RouteStopRepository extends JpaRepository<RouteStop, Integer> {
    // Busca por ID y ordena por la posición del paradero
    @Query("SELECT rs FROM RouteStop rs JOIN FETCH rs.stop WHERE rs.route.id = :routeId ORDER BY rs.stopOrder ASC")
    List<RouteStop> findByRouteIdWithStops(@Param("routeId") Integer routeId);
}
