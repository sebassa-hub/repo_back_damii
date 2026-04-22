package com.rutasproyect.damii.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.rutasproyect.damii.model.FavoriteRoute;
import com.rutasproyect.damii.model.RouteComment;
import com.rutasproyect.damii.model.RouteRating;
import com.rutasproyect.damii.model.TransportRoute;
import com.rutasproyect.damii.model.User;
import com.rutasproyect.damii.model.UserComment;
import com.rutasproyect.damii.repository.FavoriteRouteRepository;
import com.rutasproyect.damii.repository.RouteCommentRepository;
import com.rutasproyect.damii.repository.RouteRatingRepository;
import com.rutasproyect.damii.repository.TransportRouteRepository;
import com.rutasproyect.damii.repository.UserCommentRepository;
import com.rutasproyect.damii.repository.UserRepository;

@Service
public class InteractionService {

    private final FavoriteRouteRepository favoriteRouteRepository;
    private final RouteCommentRepository routeCommentRepository;
    private final UserCommentRepository userCommentRepository;
    private final RouteRatingRepository routeRatingRepository;
    private final TransportRouteRepository transportRouteRepository;
    private final UserRepository userRepository;

    public InteractionService(FavoriteRouteRepository favoriteRouteRepository,
                              RouteCommentRepository routeCommentRepository,
                              UserCommentRepository userCommentRepository,
                              RouteRatingRepository routeRatingRepository,
                              TransportRouteRepository transportRouteRepository,
                              UserRepository userRepository) {
        this.favoriteRouteRepository = favoriteRouteRepository;
        this.routeCommentRepository = routeCommentRepository;
        this.userCommentRepository = userCommentRepository;
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

        return favoriteRouteRepository.findByUserIdAndRouteId(user.getId(), route.getId())
                .map(existingFavorite -> {
                    favoriteRouteRepository.delete(existingFavorite);
                    return null; // Devuelve null si lo elimina (toggle off)
                })
                .orElseGet(() -> {
                    FavoriteRoute newFav = new FavoriteRoute();
                    newFav.setUser(user);
                    newFav.setRoute(route);
                    return favoriteRouteRepository.save(newFav); // Devuelve la entidad si lo crea (toggle on)
                });
    }

    public List<FavoriteRoute> getUserFavorites(String userEmail) {
        User user = getUserByEmail(userEmail);
        return favoriteRouteRepository.findByUserId(user.getId());
    }

    // --- USER/COMPANY COMMENTS ---
    public UserComment addCompanyComment(String userEmail, String network, String commentText) {
        User user = getUserByEmail(userEmail);
        UserComment comment = new UserComment();
        comment.setUser(user);
        comment.setNetwork(network);
        comment.setComment(commentText);
        return userCommentRepository.save(comment);
    }

    // --- ROUTE COMMENTS ---
    public RouteComment addRouteComment(String userEmail, Integer routeId, String commentText) {
        User user = getUserByEmail(userEmail);
        TransportRoute route = getRouteById(routeId);
        
        RouteComment comment = new RouteComment();
        comment.setUser(user);
        comment.setRoute(route);
        comment.setComment(commentText);
        return routeCommentRepository.save(comment);
    }

    // --- ROUTE RATINGS ---
    public RouteRating addRouteRating(String userEmail, Integer routeId, Integer ratingScore) {
        User user = getUserByEmail(userEmail);
        TransportRoute route = getRouteById(routeId);
        
        RouteRating rating = new RouteRating();
        rating.setUser(user);
        rating.setRoute(route);
        rating.setRating(ratingScore);
        return routeRatingRepository.save(rating);
    }
}
