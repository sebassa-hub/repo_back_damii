package com.rutasproyect.damii.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rutasproyect.damii.dto.CommentRequestDTO;
import com.rutasproyect.damii.dto.RatingRequestDTO;
import com.rutasproyect.damii.model.FavoriteRoute;
import com.rutasproyect.damii.model.RouteComment;
import com.rutasproyect.damii.model.RouteRating;
import com.rutasproyect.damii.model.UserComment;
import com.rutasproyect.damii.service.InteractionService;

@RestController
@RequestMapping("/api/v1/mobile/private")
public class UserInteractionController {

    private final InteractionService interactionService;

    public UserInteractionController(InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    // Favoritos
    @PostMapping("/favorites/{routeId}")
    public ResponseEntity<?> toggleFavorite(Authentication authentication, @PathVariable Integer routeId) {
        String userEmail = authentication.getName();
        FavoriteRoute fav = interactionService.toggleFavoriteRoute(userEmail, routeId);
        if (fav == null) {
            return ResponseEntity.ok(Map.of("message", "Removed from favorites"));
        }
        return ResponseEntity.ok(fav);
    }

    @GetMapping("/favorites")
    public ResponseEntity<List<FavoriteRoute>> getFavorites(Authentication authentication) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(interactionService.getUserFavorites(userEmail));
    }

    // Comentarios de la empresa (network)
    @PostMapping("/comments/company")
    public ResponseEntity<UserComment> commentCompany(Authentication authentication, 
                                                      @RequestParam String network, 
                                                      @RequestBody CommentRequestDTO request) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(interactionService.addCompanyComment(userEmail, network, request.getComment()));
    }

    // Comentarios de rutas
    @PostMapping("/comments/route/{routeId}")
    public ResponseEntity<RouteComment> commentRoute(Authentication authentication, 
                                                     @PathVariable Integer routeId, 
                                                     @RequestBody CommentRequestDTO request) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(interactionService.addRouteComment(userEmail, routeId, request.getComment()));
    }

    // Ratings de rutas (nivel de peligrosidad)
    @PostMapping("/ratings/route/{routeId}")
    public ResponseEntity<RouteRating> rateRoute(Authentication authentication, 
                                                 @PathVariable Integer routeId, 
                                                 @RequestBody RatingRequestDTO request) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(interactionService.addRouteRating(userEmail, routeId, request.getRating()));
    }
}
