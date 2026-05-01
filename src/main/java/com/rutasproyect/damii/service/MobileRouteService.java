package com.rutasproyect.damii.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.rutasproyect.damii.dto.RouteBasicInfoDTO;
import com.rutasproyect.damii.dto.RouteCoordinateDTO;
import com.rutasproyect.damii.dto.RouteDetailDTO;
import com.rutasproyect.damii.dto.RouteSummaryDTO;
import com.rutasproyect.damii.model.RouteShape;
import com.rutasproyect.damii.model.RouteStop;
import com.rutasproyect.damii.model.TransportRoute;
import com.rutasproyect.damii.repository.RouteShapeRepository;
import com.rutasproyect.damii.repository.RouteStopRepository;
import com.rutasproyect.damii.repository.TransportRouteRepository;
import com.rutasproyect.damii.service.MobileRouteService.RouteStopDTO;

import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true) // Optimiza las consultas al no bloquear la base de datos
public class MobileRouteService {

        private final TransportRouteRepository routeRepository;
        private final RouteShapeRepository shapeRepository;
        private final RouteStopRepository routeStopRepository;

        public MobileRouteService(TransportRouteRepository routeRepository,
                        RouteShapeRepository shapeRepository,
                        RouteStopRepository routeStopRepository) {
                this.routeRepository = routeRepository;
                this.shapeRepository = shapeRepository;
                this.routeStopRepository = routeStopRepository;
        }

        // 1. Buscador rápido para la App (Filtrado para enviar SOLO rutas con
        // paraderos)
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

        // 2. Obtener rutas por tipo (Autobus, Corredor, Metro) o Todas
        @org.springframework.cache.annotation.Cacheable("mobileRoutesNetwork")
        public List<RouteSummaryDTO> getRoutesByNetwork(String network) {
                List<TransportRoute> routes;
                if (network == null || network.trim().isEmpty() || network.equalsIgnoreCase("Todos")) {
                        routes = routeRepository.findAllActiveRoutesWithStops();
                } else {
                        routes = routeRepository.findActiveRoutesWithStopsByNetwork(network);
                }

                return routes.stream()
                                .map(r -> new RouteSummaryDTO(
                                                r.getId(), r.getName(), r.getRouteRef(),
                                                r.getNetwork(), r.getIsVerified(), r.getRiskLevel()))
                                .toList();
        }

        // 3. Buscar rutas específicamente por nombre de empresa
        public List<RouteSummaryDTO> getRoutesByCompanyName(String name) {
                List<TransportRoute> routes = routeRepository.findActiveRoutesWithStopsByNameContaining(name);
                return routes.stream()
                                .map(r -> new RouteSummaryDTO(
                                                r.getId(), r.getName(), r.getRouteRef(),
                                                r.getNetwork(), r.getIsVerified(), r.getRiskLevel()))
                                .toList();
        }

        // 4. Obtener información básica de la ruta para el Detalle
        public RouteBasicInfoDTO getRouteBasicInfo(Integer routeId) {
                TransportRoute route = routeRepository.findById(routeId)
                                .orElseThrow(() -> new RuntimeException("Ruta no encontrada"));
                
                return new RouteBasicInfoDTO(route.getId(), route.getName(), route.getRouteRef());
        }

        // 5. Obtener los puntos para dibujar la línea en MapKit
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

        // DTO Ligero para enviar solo lo necesario del paradero
        public record RouteStopDTO(
                        Integer id, String name, Double latitude, Double longitude, Integer stopOrder) {
        }

        // Nuevo método en el Service
        public List<RouteStopDTO> getRouteStops(Integer routeId) {

                // Usamos TU consulta que trae todo en 1 solo paso y ya ordenado
                List<RouteStop> routeStops = routeStopRepository.findByRouteIdWithStops(routeId);

                // Si la lista está vacía, o la ruta no existe o no tiene paraderos
                if (routeStops.isEmpty()) {
                        throw new RuntimeException("Ruta no encontrada o sin paraderos asignados");
                }

                // Mapeamos a DTO para enviar un JSON limpio al iPhone
                return routeStops.stream()
                                .map(rs -> new RouteStopDTO(
                                                rs.getStop().getId(),
                                                rs.getStop().getName(),
                                                rs.getStop().getLatitude(),
                                                rs.getStop().getLongitude(),
                                                rs.getStopOrder()))
                                .toList();
        }
}
