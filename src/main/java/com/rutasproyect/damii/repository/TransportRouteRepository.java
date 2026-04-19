package com.rutasproyect.damii.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rutasproyect.damii.model.TransportRoute;

import java.util.List;

@Repository
public interface TransportRouteRepository extends JpaRepository<TransportRoute, Integer> {

    // Para el buscador de texto en la app (ej: "Corredor", "301")
    List<TransportRoute> findByNameContainingIgnoreCaseOrRouteRefContainingIgnoreCase(String name, String ref);

    List<TransportRoute> findByIsVerifiedTrue();
}