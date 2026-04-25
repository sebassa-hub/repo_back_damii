package com.rutasproyect.damii.controller;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rutasproyect.damii.model.RouteRating;
import com.rutasproyect.damii.model.RouteStop;
import com.rutasproyect.damii.model.TransportRoute;
import com.rutasproyect.damii.model.User;
import com.rutasproyect.damii.repository.RouteRatingRepository;
import com.rutasproyect.damii.repository.RouteStopRepository;
import com.rutasproyect.damii.repository.TransportRouteRepository;
import com.rutasproyect.damii.repository.UserRepository;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminWebController {

    private final UserRepository userRepository;
    private final TransportRouteRepository routeRepository;
    private final RouteRatingRepository ratingRepository;
    private final RouteStopRepository routeStopRepository;
    private final com.rutasproyect.damii.repository.StopRepository stopRepository;

    public AdminWebController(UserRepository userRepository,
            TransportRouteRepository routeRepository,
            RouteRatingRepository ratingRepository,
            RouteStopRepository routeStopRepository,
            com.rutasproyect.damii.repository.StopRepository stopRepository) {
        this.userRepository = userRepository;
        this.routeRepository = routeRepository;
        this.ratingRepository = ratingRepository;
        this.routeStopRepository = routeStopRepository;
        this.stopRepository = stopRepository;
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/routes")
    public ResponseEntity<Page<TransportRoute>> getAllRoutes(
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        Pageable pageable = PageRequest.of(page, size);

        if (search != null && !search.trim().isEmpty()) {
            return ResponseEntity.ok(routeRepository.findByNameContainingIgnoreCaseOrRouteRefContainingIgnoreCase(search, search, pageable));
        }

        if ("with_stops".equalsIgnoreCase(filter)) {
            return ResponseEntity.ok(routeRepository.findAllRoutesWithStops(pageable));
        } else if ("without_stops".equalsIgnoreCase(filter)) {
            return ResponseEntity.ok(routeRepository.findAllRoutesWithoutStops(pageable));
        }
        return ResponseEntity.ok(routeRepository.findAll(pageable));
    }

    @PutMapping("/routes/{id}")
    @CacheEvict(value = "mobileRoutesSearch", allEntries = true)
    public ResponseEntity<TransportRoute> updateRoute(@PathVariable Integer id, @RequestBody TransportRoute data) {
        return routeRepository.findById(id).map(existing -> {
            existing.setName(data.getName());
            existing.setNetwork(data.getNetwork());
            existing.setRouteRef(data.getRouteRef());
            existing.setIsVerified(data.getIsVerified());
            existing.setRiskLevel(data.getRiskLevel());
            return ResponseEntity.ok(routeRepository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/routes/{id}")
    @CacheEvict(value = "mobileRoutesSearch", allEntries = true)
    public ResponseEntity<Void> deleteRoute(@PathVariable Integer id) {
        if (!routeRepository.existsById(id))
            return ResponseEntity.notFound().build();
        routeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/routes/{id}/stops")
    public ResponseEntity<List<RouteStop>> getRouteStops(@PathVariable Integer id) {
        return ResponseEntity.ok(routeStopRepository.findByRouteIdWithStops(id));
    }

    @PutMapping("/stops/{id}")
    @CacheEvict(value = "mobileRoutesSearch", allEntries = true)
    public ResponseEntity<com.rutasproyect.damii.model.Stop> updateStop(@PathVariable Integer id,
            @RequestBody com.rutasproyect.damii.model.Stop data) {
        return stopRepository.findById(id).map(existing -> {
            existing.setName(data.getName());
            existing.setLatitude(data.getLatitude());
            existing.setLongitude(data.getLongitude());
            return ResponseEntity.ok(stopRepository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/route-stops/{id}")
    @CacheEvict(value = "mobileRoutesSearch", allEntries = true)
    public ResponseEntity<Void> deleteRouteStop(@PathVariable Integer id) {
        if (!routeStopRepository.existsById(id))
            return ResponseEntity.notFound().build();
        routeStopRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/route-stops/{id}")
    @CacheEvict(value = "mobileRoutesSearch", allEntries = true)
    public ResponseEntity<RouteStop> updateRouteStop(@PathVariable Integer id, @RequestBody RouteStop data) {
        return routeStopRepository.findById(id).map(existing -> {
            existing.setStopOrder(data.getStopOrder());
            existing.setDirection(data.getDirection());
            return ResponseEntity.ok(routeStopRepository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    public static class CreateStopRequest {
        public String name;
        public Double latitude;
        public Double longitude;
        public Integer stopOrder;
        public String direction;
    }

    @org.springframework.web.bind.annotation.PostMapping("/routes/{id}/stops")
    @CacheEvict(value = "mobileRoutesSearch", allEntries = true)
    public ResponseEntity<RouteStop> addStopToRoute(@PathVariable Integer id, @RequestBody CreateStopRequest req) {
        return routeRepository.findById(id).map(route -> {
            com.rutasproyect.damii.model.Stop newStop = new com.rutasproyect.damii.model.Stop();
            newStop.setName(req.name);
            newStop.setLatitude(req.latitude);
            newStop.setLongitude(req.longitude);
            stopRepository.save(newStop);

            RouteStop rs = new RouteStop();
            rs.setRoute(route);
            rs.setStop(newStop);
            rs.setStopOrder(req.stopOrder);
            rs.setDirection(req.direction);
            routeStopRepository.save(rs);

            return ResponseEntity.ok(rs);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/ratings")
    public ResponseEntity<List<RouteRating>> getAllRatings() {
        return ResponseEntity.ok(ratingRepository.findAll());
    }

}
