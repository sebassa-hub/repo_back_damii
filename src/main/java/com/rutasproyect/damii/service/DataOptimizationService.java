package com.rutasproyect.damii.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Async;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.rutasproyect.damii.dto.ProgressInfo;
import com.rutasproyect.damii.model.RouteShape;
import com.rutasproyect.damii.model.RouteStop;
import com.rutasproyect.damii.model.Stop;
import com.rutasproyect.damii.model.TransportRoute;
import com.rutasproyect.damii.repository.RouteShapeRepository;
import com.rutasproyect.damii.repository.RouteStopRepository;
import com.rutasproyect.damii.repository.StopRepository;
import com.rutasproyect.damii.repository.TransportRouteRepository;

@Service
public class DataOptimizationService {

    private final StopRepository stopRepository;
    private final RouteStopRepository routeStopRepository;
    private final RouteShapeRepository routeShapeRepository;
    private final TransportRouteRepository transportRouteRepository;
    private final RestTemplate restTemplate;

    // Tracker de progreso
    private final Map<String, ProgressInfo> progressMap = new ConcurrentHashMap<>();

    public ProgressInfo getProgress(String taskId) {
        return progressMap.getOrDefault(taskId, new ProgressInfo("NOT_STARTED", 0, 0, "No process running"));
    }

    public DataOptimizationService(StopRepository stopRepository,
                                   RouteStopRepository routeStopRepository,
                                   RouteShapeRepository routeShapeRepository,
                                   TransportRouteRepository transportRouteRepository) {
        this.stopRepository = stopRepository;
        this.routeStopRepository = routeStopRepository;
        this.routeShapeRepository = routeShapeRepository;
        this.transportRouteRepository = transportRouteRepository;
        this.restTemplate = new RestTemplate();
    }

    /**
     * 1. Reverse Geocoding para limpiar nombres de paraderos.
     */
    @Transactional
    public int fixStopNames() {
        List<Stop> allStops = stopRepository.findAll();
        int updatedCount = 0;

        for (Stop stop : allStops) {
            String name = stop.getName();
            // Si el nombre es solo números, empieza con Node:, o está vacío
            if (name == null || name.matches("\\d+") || name.startsWith("Node:")) {
                try {
                    String url = String.format(
                            "https://nominatim.openstreetmap.org/reverse?lat=%s&lon=%s&format=json",
                            stop.getLatitude(), stop.getLongitude());

                    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                    headers.set("User-Agent", "DamiiTransitApp/1.0 (contacto@rutasproyect.com)");
                    org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

                    ResponseEntity<Map> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, Map.class);
                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        Map<String, Object> body = response.getBody();
                        Map<String, String> address = (Map<String, String>) body.get("address");

                        String newName = null;
                        if (address != null) {
                            if (address.containsKey("road")) {
                                newName = address.get("road");
                            } else if (address.containsKey("neighbourhood")) {
                                newName = address.get("neighbourhood");
                            }
                        }

                        if (newName == null && body.containsKey("display_name")) {
                            String displayName = (String) body.get("display_name");
                            // Tomamos la primera parte antes de la coma
                            newName = displayName.split(",")[0].trim();
                        }

                        if (newName != null && !newName.isEmpty()) {
                            stop.setName(newName);
                            stopRepository.save(stop);
                            updatedCount++;
                        }
                    }

                    // Pausa de 1 segundo para no saturar Nominatim (política de uso)
                    Thread.sleep(1000);
                } catch (Exception e) {
                    System.err.println("Error procesando stop ID " + stop.getId() + ": " + e.getMessage());
                }
            }
        }
        return updatedCount;
    }

    /**
     * 2. Separar Ida y Vuelta
     */
    @Transactional
    public void splitRouteDirections(Integer routeId) {
        List<RouteStop> routeStops = routeStopRepository.findByRouteIdWithStops(routeId);
        if (routeStops.isEmpty() || routeStops.size() < 2) return;

        // Ordenar por el orden definido
        routeStops.sort(Comparator.comparing(RouteStop::getStopOrder, Comparator.nullsLast(Comparator.naturalOrder())));

        Stop firstStop = routeStops.get(0).getStop();
        double maxDistance = -1;
        int turnaroundIndex = -1;

        // Encontrar el paradero más lejano geográficamente al origen
        for (int i = 1; i < routeStops.size(); i++) {
            Stop currentStop = routeStops.get(i).getStop();
            double distance = haversine(firstStop.getLatitude(), firstStop.getLongitude(),
                                        currentStop.getLatitude(), currentStop.getLongitude());
            if (distance > maxDistance) {
                maxDistance = distance;
                turnaroundIndex = i;
            }
        }

        // Asignar "ida" y "vuelta"
        for (int i = 0; i < routeStops.size(); i++) {
            RouteStop rs = routeStops.get(i);
            if (i <= turnaroundIndex) {
                rs.setDirection("ida");
            } else {
                rs.setDirection("vuelta");
            }
            routeStopRepository.save(rs);
        }
    }

    /**
     * 3. Map-Matching usando OSRM
     */
    @Transactional
    public void matchRouteGeometry(Integer routeId) {
        List<RouteStop> routeStops = routeStopRepository.findByRouteIdWithStops(routeId);
        if (routeStops.isEmpty()) return;

        // Ordenamos los stops
        routeStops.sort(Comparator.comparing(RouteStop::getStopOrder, Comparator.nullsLast(Comparator.naturalOrder())));

        // Tomamos máximo 45 puntos para no exceder los límites de tiempo de OSRM gratuito
        int limit = Math.min(routeStops.size(), 45);
        
        StringBuilder coordinatesUrl = new StringBuilder();
        for (int i = 0; i < limit; i++) {
            Stop stop = routeStops.get(i).getStop();
            coordinatesUrl.append(stop.getLongitude()).append(",").append(stop.getLatitude());
            if (i < limit - 1) {
                coordinatesUrl.append(";");
            }
        }

        try {
            String url = "http://router.project-osrm.org/match/v1/driving/" + coordinatesUrl.toString() + "?geometries=geojson&overview=full";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> matchings = (List<Map<String, Object>>) body.get("matchings");

                if (matchings != null && !matchings.isEmpty()) {
                    // Borrar geometrías viejas
                    routeShapeRepository.deleteByRouteId(routeId);

                    Map<String, Object> geometry = (Map<String, Object>) matchings.get(0).get("geometry");
                    List<List<Double>> coordinates = (List<List<Double>>) geometry.get("coordinates");

                    TransportRoute route = transportRouteRepository.findById(routeId).orElseThrow();
                    List<RouteShape> newShapes = new ArrayList<>();
                    
                    int order = 1;
                    for (List<Double> coord : coordinates) {
                        RouteShape shape = new RouteShape();
                        shape.setRoute(route);
                        // GeoJSON devuelve [longitude, latitude]
                        shape.setLongitude(coord.get(0));
                        shape.setLatitude(coord.get(1));
                        shape.setSequenceOrder(order++);
                        newShapes.add(shape);
                    }
                    
                    routeShapeRepository.saveAll(newShapes);
                }
            }
        } catch (Exception e) {
            System.err.println("Error procesando map-matching para route ID " + routeId + ": " + e.getMessage());
            throw new RuntimeException("Map-matching failed", e);
        }
    }

    // --- Helpers ---
    
    // Fórmula Haversine para calcular distancia entre dos coordenadas en metros
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Radio de la Tierra en metros
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // --- Async Global Runners ---

    @Async
    public void runGlobalStopNames() {
        String taskId = "global_stop_names";
        List<Stop> allStops = stopRepository.findAll();
        
        // Contamos cuántos necesitan actualización
        List<Stop> stopsToFix = allStops.stream()
            .filter(s -> s.getName() == null || s.getName().matches("\\d+") || s.getName().startsWith("Node:"))
            .collect(Collectors.toList());

        progressMap.put(taskId, new ProgressInfo("RUNNING", 0, stopsToFix.size(), "Iniciando renombrado..."));

        int updatedCount = 0;
        for (int i = 0; i < stopsToFix.size(); i++) {
            Stop stop = stopsToFix.get(i);
            try {
                String url = String.format("https://nominatim.openstreetmap.org/reverse?lat=%s&lon=%s&format=json", stop.getLatitude(), stop.getLongitude());
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.set("User-Agent", "DamiiTransitApp/1.0 (contacto@rutasproyect.com)");
                org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
                ResponseEntity<Map> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, Map.class);
                
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map<String, Object> body = response.getBody();
                    Map<String, String> address = (Map<String, String>) body.get("address");
                    String newName = null;
                    if (address != null) {
                        if (address.containsKey("road")) newName = address.get("road");
                        else if (address.containsKey("neighbourhood")) newName = address.get("neighbourhood");
                    }
                    if (newName == null && body.containsKey("display_name")) {
                        String displayName = (String) body.get("display_name");
                        newName = displayName.split(",")[0].trim();
                    }
                    if (newName != null && !newName.isEmpty()) {
                        stop.setName(newName);
                        stopRepository.save(stop);
                    }
                }
                Thread.sleep(1000); // Pausa API obligatoria
            } catch (Exception e) {
                // Ignore error for individual stop
            }
            
            updatedCount++;
            progressMap.put(taskId, new ProgressInfo("RUNNING", updatedCount, stopsToFix.size(), "Renombrando paraderos..."));
        }

        progressMap.put(taskId, new ProgressInfo("COMPLETED", updatedCount, stopsToFix.size(), "Renombrado finalizado"));
    }

    @Async
    public void runGlobalRouteSplit() {
        String taskId = "global_route_split";
        // Obtenemos solo rutas que sí tienen paraderos
        List<TransportRoute> routes = transportRouteRepository.findAllRoutesWithStops(org.springframework.data.domain.Pageable.unpaged()).getContent();
        
        progressMap.put(taskId, new ProgressInfo("RUNNING", 0, routes.size(), "Iniciando separación de ida y vuelta..."));

        int count = 0;
        for (TransportRoute route : routes) {
            try {
                splitRouteDirections(route.getId());
            } catch(Exception e) {
                // Ignore
            }
            count++;
            progressMap.put(taskId, new ProgressInfo("RUNNING", count, routes.size(), "Separando rutas..."));
        }
        
        progressMap.put(taskId, new ProgressInfo("COMPLETED", count, routes.size(), "Separación de rutas finalizada"));
    }

    @Async
    public void runGlobalRouteMatch() {
        String taskId = "global_route_match";
        List<TransportRoute> routes = transportRouteRepository.findAllRoutesWithStops(org.springframework.data.domain.Pageable.unpaged()).getContent();
        
        progressMap.put(taskId, new ProgressInfo("RUNNING", 0, routes.size(), "Iniciando emparejamiento geométrico (Map-Matching)..."));

        int count = 0;
        for (TransportRoute route : routes) {
            try {
                matchRouteGeometry(route.getId());
                Thread.sleep(1500); // Mayor pausa para no saturar OSRM público
            } catch(Exception e) {
                // Ignore
            }
            count++;
            progressMap.put(taskId, new ProgressInfo("RUNNING", count, routes.size(), "Alineando rutas con calles..."));
        }
        
        progressMap.put(taskId, new ProgressInfo("COMPLETED", count, routes.size(), "Alineación de rutas finalizada"));
    }
}
