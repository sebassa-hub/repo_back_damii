package com.rutasproyect.damii.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.rutasproyect.damii.dto.RouteBasicInfoDTO;
import com.rutasproyect.damii.dto.RouteDetailDTO;
import com.rutasproyect.damii.dto.RouteSummaryDTO;
import com.rutasproyect.damii.service.MobileRouteService;
import com.rutasproyect.damii.service.MobileRouteService.RouteStopDTO;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mobile/public")
public class MobileRouteController {

    private final MobileRouteService routeService;

    public MobileRouteController(MobileRouteService routeService) {
        this.routeService = routeService;
    }

    // GET /api/v1/mobile/routes/search?q=Corredor
    @GetMapping("/routes/search")
    public ResponseEntity<List<RouteSummaryDTO>> search(@RequestParam String q) {
        return ResponseEntity.ok(routeService.searchRoutes(q));
    }

    // GET /api/v1/mobile/routes?network=Autobus
    @GetMapping("/routes")
    public ResponseEntity<List<RouteSummaryDTO>> getRoutesByNetwork(
            @RequestParam(required = false, defaultValue = "Todos") String network) {
        return ResponseEntity.ok(routeService.getRoutesByNetwork(network));
    }

    // GET /api/v1/mobile/routes/company/search?name=Empresa
    @GetMapping("/routes/company/search")
    public ResponseEntity<List<RouteSummaryDTO>> searchByCompany(@RequestParam String name) {
        return ResponseEntity.ok(routeService.getRoutesByCompanyName(name));
    }

    // GET /api/v1/mobile/routes/123/info
    @GetMapping("/routes/{id}/info")
    public ResponseEntity<RouteBasicInfoDTO> getRouteInfo(@PathVariable Integer id) {
        return ResponseEntity.ok(routeService.getRouteBasicInfo(id));
    }

    // GET /api/v1/mobile/routes/123/map
    @GetMapping("/routes/{id}/map")
    public ResponseEntity<RouteDetailDTO> getRouteForMap(@PathVariable Integer id) {
        return ResponseEntity.ok(routeService.getRouteDetailsForMap(id));
    }

    @GetMapping("/routes/{id}/stops")
    public ResponseEntity<List<RouteStopDTO>> getStops(@PathVariable Integer id) {
        return ResponseEntity.ok(routeService.getRouteStops(id));
    }

    /*
     * // GET /api/v1/mobile/radar?lat=-12.04&lng=-77.02&radius=500
     * 
     * @GetMapping("/radar")
     * public ResponseEntity<List<NearbyIncidentDTO>> getRadar(
     * 
     * @RequestParam Double lat,
     * 
     * @RequestParam Double lng,
     * 
     * @RequestParam(defaultValue = "500") Double radius) {
     * return ResponseEntity.ok(routeService.getSecurityRadar(lat, lng, radius));
     * }
     */
}