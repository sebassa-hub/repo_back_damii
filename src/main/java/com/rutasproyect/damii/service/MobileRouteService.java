package com.rutasproyect.damii.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.rutasproyect.damii.dto.RouteCoordinateDTO;
import com.rutasproyect.damii.dto.RouteDetailDTO;
import com.rutasproyect.damii.dto.RouteSummaryDTO;
import com.rutasproyect.damii.model.RouteShape;
import com.rutasproyect.damii.model.TransportRoute;
import com.rutasproyect.damii.repository.RouteShapeRepository;
import com.rutasproyect.damii.repository.TransportRouteRepository;

import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true) // Optimiza las consultas al no bloquear la base de datos
public class MobileRouteService {

    private final TransportRouteRepository routeRepository;
    private final RouteShapeRepository shapeRepository;

    public MobileRouteService(TransportRouteRepository routeRepository,
            RouteShapeRepository shapeRepository) {
        this.routeRepository = routeRepository;
        this.shapeRepository = shapeRepository;
    }

    // 1. Buscador rápido para la App (Filtrado para enviar SOLO rutas con paraderos)
    @org.springframework.cache.annotation.Cacheable("mobileRoutesSearch")
    public List<RouteSummaryDTO> searchRoutes(String query) {
        List<TransportRoute> routes = routeRepository
                .searchActiveRoutesWithStops(query, query);

        return routes.stream()
                .map(r -> new RouteSummaryDTO(
                        r.getId(), r.getName(), r.getRouteRef(),
                        r.getNetwork(), r.getIsVerified(), r.getRiskLevel()))
                .toList();
    }

    // 2. Obtener los puntos para dibujar la línea en MapKit
    public RouteDetailDTO getRouteDetailsForMap(Integer routeId) {
        TransportRoute route = routeRepository.findById(routeId)
                .orElseThrow(() -> new RuntimeException("Ruta no encontrada"));

        // Aquí usamos el método ordenado que creamos en el Repository
        List<RouteShape> shapes = shapeRepository.findByRouteIdOrderBySequenceOrderAsc(routeId);

        List<RouteCoordinateDTO> coordinates = shapes.stream()
                .map(s -> new RouteCoordinateDTO(s.getLatitude(), s.getLongitude()))
                .toList();

        return new RouteDetailDTO(route.getId(), route.getName(), route.getRiskLevel(), coordinates);
    }
}
