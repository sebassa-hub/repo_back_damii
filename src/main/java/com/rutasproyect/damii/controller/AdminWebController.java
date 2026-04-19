package com.rutasproyect.damii.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    public AdminWebController(UserRepository userRepository, 
                              TransportRouteRepository routeRepository, 
                              RouteRatingRepository ratingRepository,
                              RouteStopRepository routeStopRepository) {
        this.userRepository = userRepository;
        this.routeRepository = routeRepository;
        this.ratingRepository = ratingRepository;
        this.routeStopRepository = routeStopRepository;
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/routes")
    public ResponseEntity<List<TransportRoute>> getAllRoutes() {
        return ResponseEntity.ok(routeRepository.findAll());
    }

    @PutMapping("/routes/{id}")
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
    public ResponseEntity<Void> deleteRoute(@PathVariable Integer id) {
        if (!routeRepository.existsById(id)) return ResponseEntity.notFound().build();
        routeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/routes/{id}/stops")
    public ResponseEntity<List<RouteStop>> getRouteStops(@PathVariable Integer id) {
        return ResponseEntity.ok(routeStopRepository.findByRouteId(id));
    }

    @GetMapping("/ratings")
    public ResponseEntity<List<RouteRating>> getAllRatings() {
        return ResponseEntity.ok(ratingRepository.findAll());
    }
}
