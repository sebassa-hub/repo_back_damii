package com.rutasproyect.damii.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rutasproyect.damii.model.TransportRoute;

import java.util.List;

@Repository
public interface TransportRouteRepository extends JpaRepository<TransportRoute, Integer> {

    // Para el buscador de texto en la app móvil (Filtra solo las rutas que SI tienen paraderos)
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT r FROM TransportRoute r INNER JOIN RouteStop rs ON rs.route = r WHERE LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(r.routeRef) LIKE LOWER(CONCAT('%', :ref, '%'))")
    List<TransportRoute> searchActiveRoutesWithStops(@org.springframework.data.repository.query.Param("name") String name, @org.springframework.data.repository.query.Param("ref") String ref);

    // Filtros directos para panel administrador (Paginados)
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT r FROM TransportRoute r INNER JOIN RouteStop rs ON rs.route = r")
    org.springframework.data.domain.Page<TransportRoute> findAllRoutesWithStops(org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM TransportRoute r WHERE NOT EXISTS (SELECT 1 FROM RouteStop rs WHERE rs.route = r)")
    org.springframework.data.domain.Page<TransportRoute> findAllRoutesWithoutStops(org.springframework.data.domain.Pageable pageable);

    List<TransportRoute> findByIsVerifiedTrue();

    org.springframework.data.domain.Page<TransportRoute> findByNameContainingIgnoreCaseOrRouteRefContainingIgnoreCase(String name, String ref, org.springframework.data.domain.Pageable pageable);
}