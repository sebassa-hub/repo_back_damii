package com.rutasproyect.damii.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rutasproyect.damii.model.RouteShape;

import java.util.List;

@Repository
public interface RouteShapeRepository extends JpaRepository<RouteShape, Integer> {

    // Spring deduce automáticamente el query:
    // SELECT * FROM route_shapes WHERE route_id = ? ORDER BY sequence_order ASC
    List<RouteShape> findByRouteIdOrderBySequenceOrderAsc(Integer routeId);
    
    long countByRouteId(Integer routeId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM RouteShape rs WHERE rs.route.id = :routeId")
    void deleteByRouteId(@org.springframework.data.repository.query.Param("routeId") Integer routeId);
}