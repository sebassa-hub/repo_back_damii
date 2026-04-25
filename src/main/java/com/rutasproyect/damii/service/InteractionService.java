package com.rutasproyect.damii.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.rutasproyect.damii.model.FavoriteRoute;
import com.rutasproyect.damii.model.RouteRating;
import com.rutasproyect.damii.model.TransportRoute;
import com.rutasproyect.damii.model.User;
import com.rutasproyect.damii.repository.FavoriteRouteRepository;
import com.rutasproyect.damii.repository.RouteRatingRepository;
import com.rutasproyect.damii.repository.TransportRouteRepository;
import com.rutasproyect.damii.repository.UserRepository;

@Service
public class InteractionService {

    private final FavoriteRouteRepository favoriteRouteRepository;
    private final RouteRatingRepository routeRatingRepository;
    private final TransportRouteRepository transportRouteRepository;
    private final UserRepository userRepository;

    public InteractionService(FavoriteRouteRepository favoriteRouteRepository,
                              RouteRatingRepository routeRatingRepository,
                              TransportRouteRepository transportRouteRepository,
                              UserRepository userRepository) {
        this.favoriteRouteRepository = favoriteRouteRepository;
        this.routeRatingRepository = routeRatingRepository;
        this.transportRouteRepository = transportRouteRepository;
        this.userRepository = userRepository;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
    }

    private TransportRoute getRouteById(Integer routeId) {
        return transportRouteRepository.findById(routeId).orElseThrow(() -> new RuntimeException("Route not found"));
    }

    // --- FAVORITES ---
    public FavoriteRoute toggleFavoriteRoute(String userEmail, Integer routeId) {
        User user = getUserByEmail(userEmail);
        TransportRoute route = getRouteById(routeId);

        var existingFavorite = favoriteRouteRepository.findByUserIdAndRouteId(user.getId(), route.getId());
        
        if (existingFavorite.isPresent()) {
            favoriteRouteRepository.delete(existingFavorite.get());
            return null; // Devuelve null si lo elimina (toggle off)
        } else {
            FavoriteRoute newFav = new FavoriteRoute();
            newFav.setUser(user);
            newFav.setRoute(route);
            return favoriteRouteRepository.save(newFav); // Devuelve la entidad si lo crea (toggle on)
        }
    }

    public List<FavoriteRoute> getUserFavorites(String userEmail) {
        User user = getUserByEmail(userEmail);
        return favoriteRouteRepository.findByUserId(user.getId());
    }

    // --- ROUTE RATINGS / COMMENTS ---
    public RouteRating addRouteRating(String userEmail, Integer routeId, Integer ratingScore, String commentText) {
        User user = getUserByEmail(userEmail);
        TransportRoute route = getRouteById(routeId);
        
        RouteRating rating = new RouteRating();
        rating.setUser(user);
        rating.setRoute(route);
        rating.setRating(ratingScore);
        rating.setComment(commentText);
        return routeRatingRepository.save(rating);
    }
}
