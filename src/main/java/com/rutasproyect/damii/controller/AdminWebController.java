package com.rutasproyect.damii.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rutasproyect.damii.model.RouteRating;
import com.rutasproyect.damii.model.TransportRoute;
import com.rutasproyect.damii.model.User;
import com.rutasproyect.damii.repository.RouteRatingRepository;
import com.rutasproyect.damii.repository.TransportRouteRepository;
import com.rutasproyect.damii.repository.UserRepository;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminWebController {

    private final UserRepository userRepository;
    private final TransportRouteRepository routeRepository;
    private final RouteRatingRepository ratingRepository;

    public AdminWebController(UserRepository userRepository, 
                              TransportRouteRepository routeRepository, 
                              RouteRatingRepository ratingRepository) {
        this.userRepository = userRepository;
        this.routeRepository = routeRepository;
        this.ratingRepository = ratingRepository;
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/routes")
    public ResponseEntity<List<TransportRoute>> getAllRoutes() {
        return ResponseEntity.ok(routeRepository.findAll());
    }

    @GetMapping("/ratings")
    public ResponseEntity<List<RouteRating>> getAllRatings() {
        return ResponseEntity.ok(ratingRepository.findAll());
    }
}
