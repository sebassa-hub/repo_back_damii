package com.rutasproyect.damii.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rutasproyect.damii.model.RouteStop;
import com.rutasproyect.damii.model.Stop;
import com.rutasproyect.damii.model.TransportRoute;
import com.rutasproyect.damii.repository.RouteStopRepository;
import com.rutasproyect.damii.repository.StopRepository;
import com.rutasproyect.damii.repository.TransportRouteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.List;

import org.springframework.scheduling.annotation.Async;

@Service
public class OpenStreetMapSyncService {

    private final TransportRouteRepository routeRepository;
    private final StopRepository stopRepository;
    private final RouteStopRepository routeStopRepository;
    private final DataOptimizationService optimizationService;
    private final RestTemplate restTemplate;

    public OpenStreetMapSyncService(TransportRouteRepository routeRepository,
                                    StopRepository stopRepository,
                                    RouteStopRepository routeStopRepository,
                                    DataOptimizationService optimizationService) {
        this.routeRepository = routeRepository;
        this.stopRepository = stopRepository;
        this.routeStopRepository = routeStopRepository;
        this.optimizationService = optimizationService;
        this.restTemplate = new RestTemplate();
    }

    @Transactional
    public void syncStopsFromOverpass(Integer routeId) {
        TransportRoute route = routeRepository.findById(routeId)
                .orElseThrow(() -> new RuntimeException("Route not found"));

        String code = route.getRouteRef();
        if (code == null || code.isEmpty()) {
            throw new RuntimeException("Ruta no tiene código ref válido para buscar en OSM");
        }

        // Limpiar paraderos viejos para esta ruta específica antes de re-sincronizar
        routeStopRepository.findByRouteIdWithStops(routeId).forEach(routeStopRepository::delete);

        // Query Overpass API: Búsqueda estricta sobre relaciones de tipo bus para este REF (ej. 8611) o NAME
        String queryByRef = "[out:json];relation[\"route\"=\"bus\"][\"ref\"=\"" + code + "\"];out geom;";
        String urlByRef = "https://overpass-api.de/api/interpreter?data=" + queryByRef;

        try {
            String responseStr = restTemplate.getForObject(urlByRef, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseStr);
            JsonNode elements = root.path("elements");

            // Si por código falla, intentamos por el nombre de la empresa/ruta (ej: El Chosicano)
            if (!elements.isArray() || elements.size() == 0) {
                String nameQuery = route.getName();
                if (nameQuery != null && !nameQuery.isEmpty()) {
                    String queryByName = "[out:json];relation[\"route\"=\"bus\"][\"name\"~\"" + nameQuery + "\",i];out geom;";
                    String urlByName = "https://overpass-api.de/api/interpreter?data=" + queryByName;
                    responseStr = restTemplate.getForObject(urlByName, String.class);
                    root = mapper.readTree(responseStr);
                    elements = root.path("elements");
                }
            }

            boolean foundStops = false;
            int order = 1;

            if (elements.isArray() && elements.size() > 0) {
                // Tomamos la primera coincidencia
                JsonNode firstRelation = elements.get(0);
                JsonNode members = firstRelation.path("members");

                if (members.isArray()) {
                    for (JsonNode member : members) {
                        if ("node".equals(member.path("type").asText())) {
                            String role = member.path("role").asText();
                            // Nos interesa los paraderos, paraderos de plataforma o puntos de parada
                            if (role.contains("stop") || role.contains("platform") || role.contains("station")) {
                                double lat = member.path("lat").asDouble();
                                double lon = member.path("lon").asDouble();

                                Optional<Stop> existingStop = stopRepository.findByLatitudeAndLongitude(lat, lon);
                                Stop stop;
                                if (existingStop.isPresent()) {
                                    stop = existingStop.get();
                                } else {
                                    stop = new Stop();
                                    stop.setLatitude(lat);
                                    stop.setLongitude(lon);
                                    stop.setName("Paradero " + code + " - Orden " + order); // Temporal hasta FixStopNames
                                    stop = stopRepository.save(stop);
                                }

                                RouteStop routeStop = new RouteStop();
                                routeStop.setRoute(route);
                                routeStop.setStop(stop);
                                routeStop.setStopOrder(order++);
                                routeStop.setDirection("IDA"); // Asumimos IDA temporalmente
                                routeStopRepository.save(routeStop);

                                foundStops = true;
                            }
                        }
                    }
                }
            }

            if (foundStops) {
                // Llamamos a que la geometría se auto-genere en base a los nuevos paraderos 100% verídicos
                optimizationService.matchRouteGeometry(routeId);
                // Renombrar los paraderos temporalmente autogenerados
                optimizationService.fixStopNames();
                
                route.setIsVerified(true);
            } else {
                route.setIsVerified(false); // OSM no tiene datos completos sobre los paraderos de esta ruta
            }
            
            routeRepository.save(route);

        } catch (Exception e) {
            System.err.println("Error al sincronizar paraderos desde Overpass API para código " + code);
            e.printStackTrace();
        }
    }

    @Async
    public void runGlobalOsmSync() {
        String taskId = "global_osm_sync";
        List<TransportRoute> routes = routeRepository.findAll();
        optimizationService.getProgressMap().put(taskId, new com.rutasproyect.damii.dto.ProgressInfo("RUNNING", 0, routes.size(), "Iniciando escaneo global con Overpass (OSM)..."));

        int count = 0;
        for (TransportRoute route : routes) {
            try {
                // Solo intentar sincronizar si no tiene geometría ni paraderos verificados
                if (route.getIsVerified() == null || !route.getIsVerified()) {
                    syncStopsFromOverpass(route.getId());
                    Thread.sleep(2000); // Pausa obligatoria de 2s para Overpass
                }
            } catch(Exception e) {
                // Ignore timeout or empty routeRef
            }
            count++;
            optimizationService.getProgressMap().put(taskId, new com.rutasproyect.damii.dto.ProgressInfo("RUNNING", count, routes.size(), "Extrayendo rutas desde OpenStreetMap..."));
        }
        
        optimizationService.getProgressMap().put(taskId, new com.rutasproyect.damii.dto.ProgressInfo("COMPLETED", count, routes.size(), "Escaneo de OpenStreetMap finalizado"));
    }
}
