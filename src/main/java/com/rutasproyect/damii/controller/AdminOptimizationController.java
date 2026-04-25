package com.rutasproyect.damii.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rutasproyect.damii.service.DataOptimizationService;

@RestController
@RequestMapping("/api/v1/admin/optimize")
public class AdminOptimizationController {

    private final DataOptimizationService optimizationService;
    private final com.rutasproyect.damii.service.WikipediaScraperService scraperService;
    private final com.rutasproyect.damii.service.OpenStreetMapSyncService osmSyncService;

    public AdminOptimizationController(DataOptimizationService optimizationService, 
                                       com.rutasproyect.damii.service.WikipediaScraperService scraperService,
                                       com.rutasproyect.damii.service.OpenStreetMapSyncService osmSyncService) {
        this.optimizationService = optimizationService;
        this.scraperService = scraperService;
        this.osmSyncService = osmSyncService;
    }

    @PostMapping("/stops/names")
    public ResponseEntity<?> optimizeStopNames() {
        int updated = optimizationService.fixStopNames();
        return ResponseEntity.ok(Map.of("message", "Nombres de paraderos actualizados", "updatedCount", updated));
    }

    @PostMapping("/routes/{routeId}/split")
    public ResponseEntity<?> splitRouteDirections(@PathVariable Integer routeId) {
        optimizationService.splitRouteDirections(routeId);
        return ResponseEntity.ok(Map.of("message", "Direcciones de ruta separadas con éxito"));
    }

    @PostMapping("/routes/{routeId}/match")
    public ResponseEntity<?> matchRouteGeometry(@PathVariable Integer routeId) {
        optimizationService.matchRouteGeometry(routeId);
        return ResponseEntity.ok(Map.of("message", "Geometría de ruta emparejada usando OSRM"));
    }

    @PostMapping("/run-all/stop-names")
    public ResponseEntity<?> runGlobalStopNames() {
        optimizationService.runGlobalStopNames();
        return ResponseEntity.ok(Map.of("message", "Proceso global de renombrado iniciado"));
    }

    @PostMapping("/run-all/split-routes")
    public ResponseEntity<?> runGlobalSplitRoutes() {
        optimizationService.runGlobalRouteSplit();
        return ResponseEntity.ok(Map.of("message", "Proceso global de separación iniciado"));
    }

    @PostMapping("/run-all/match-routes")
    public ResponseEntity<?> runGlobalMatchRoutes() {
        optimizationService.runGlobalRouteMatch();
        return ResponseEntity.ok(Map.of("message", "Proceso global de map-matching iniciado"));
    }

    @PostMapping("/run-all/wiki-sync")
    public ResponseEntity<?> runWikiSync() {
        scraperService.scrapeAndSyncRoutes();
        return ResponseEntity.ok(Map.of("message", "Proceso global de sincronización de Wikipedia iniciado"));
    }

    @PostMapping("/run-all/osm-sync")
    public ResponseEntity<?> runGlobalOsmSync() {
        osmSyncService.runGlobalOsmSync();
        return ResponseEntity.ok(Map.of("message", "Escaneo masivo de rutas verdaderas en OSM iniciado"));
    }

    @PostMapping("/run-all/trace-routes")
    public ResponseEntity<?> runGlobalTraceRoutes() {
        optimizationService.runGlobalRouteTracing();
        return ResponseEntity.ok(Map.of("message", "Proceso de auto-trazado de contingencia iniciado"));
    }

    @PostMapping("/purge-osm-data")
    public ResponseEntity<?> purgeOsmData() {
        optimizationService.cleanCorruptedOsmData();
        return ResponseEntity.ok(Map.of("message", "Se eliminaron todas las rutas, formas y paraderos con éxito."));
    }

    @PostMapping("/routes/{routeId}/sync-osm-stops")
    public ResponseEntity<?> syncOsmStops(@PathVariable Integer routeId) {
        osmSyncService.syncStopsFromOverpass(routeId);
        return ResponseEntity.ok(Map.of("message", "Sincronización de paraderos desde Overpass completada. Revisa la base de datos para validar si la data era precisa."));
    }

    @GetMapping("/status/{taskId}")
    public ResponseEntity<?> getStatus(@PathVariable String taskId) {
        return ResponseEntity.ok(optimizationService.getProgress(taskId));
    }
}
