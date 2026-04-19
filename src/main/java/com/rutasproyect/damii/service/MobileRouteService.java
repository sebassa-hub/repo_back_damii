package com.rutasproyect.damii.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.rutasproyect.damii.dto.NearbyIncidentDTO;
import com.rutasproyect.damii.dto.RouteCoordinateDTO;
import com.rutasproyect.damii.dto.RouteDetailDTO;
import com.rutasproyect.damii.dto.RouteSummaryDTO;
import com.rutasproyect.damii.model.Report;
import com.rutasproyect.damii.model.RouteShape;
import com.rutasproyect.damii.model.TransportRoute;
import com.rutasproyect.damii.repository.ReportRepository;
import com.rutasproyect.damii.repository.RouteShapeRepository;
import com.rutasproyect.damii.repository.TransportRouteRepository;

import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true) // Optimiza las consultas al no bloquear la base de datos
public class MobileRouteService {

    private final TransportRouteRepository routeRepository;
    private final RouteShapeRepository shapeRepository;
    private final ReportRepository reportRepository;

    public MobileRouteService(TransportRouteRepository routeRepository,
            RouteShapeRepository shapeRepository,
            ReportRepository reportRepository) {
        this.routeRepository = routeRepository;
        this.shapeRepository = shapeRepository;
        this.reportRepository = reportRepository;
    }

    // 1. Buscador rápido para la App
    public List<RouteSummaryDTO> searchRoutes(String query) {
        List<TransportRoute> routes = routeRepository
                .findByNameContainingIgnoreCaseOrRouteRefContainingIgnoreCase(query, query);

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

    // 3. Radar de Seguridad
    public List<NearbyIncidentDTO> getSecurityRadar(Double lat, Double lng, Double radiusMeters) {
        List<Report> recentReports = reportRepository.findRecentIncidentsNearLocation(lat, lng, radiusMeters);

        return recentReports.stream()
                .map(r -> new NearbyIncidentDTO(
                        r.getReportType(),
                        r.getLocation().getY(), // Y = Latitud en JTS Point
                        r.getLocation().getX(), // X = Longitud en JTS Point
                        calculateTimeAgo(r.getReportTime())))
                .toList();
    }

    // Helper para formato amigable en iOS
    private String calculateTimeAgo(LocalDateTime reportTime) {
        long hours = Duration.between(reportTime, LocalDateTime.now()).toHours();
        if (hours == 0)
            return "Hace menos de 1 hora";
        return "Hace " + hours + " horas";
    }
}
