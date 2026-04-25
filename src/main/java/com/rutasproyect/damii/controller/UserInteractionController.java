package com.rutasproyect.damii.controller;

import java.util.List;
import java.util.Map;
import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rutasproyect.damii.model.FavoriteRoute;
import com.rutasproyect.damii.service.InteractionService;

@RestController
@RequestMapping("/api/v1/mobile/private")
public class UserInteractionController {

    private final InteractionService interactionService;

    public UserInteractionController(InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    // Favorites
    @PostMapping("/routes/{routeId}/favorite")
    public ResponseEntity<?> toggleFavorite(Principal principal,
            @PathVariable Integer routeId) {
        FavoriteRoute fav = interactionService.toggleFavoriteRoute(principal.getName(), routeId);
        if (fav == null)
            return ResponseEntity.ok(Map.of("message", "Removido de favoritos"));
        return ResponseEntity.ok(fav);
    }

    @GetMapping("/favorites")
    public ResponseEntity<List<FavoriteRoute>> getFavorites(Principal principal) {
        return ResponseEntity.ok(interactionService.getUserFavorites(principal.getName()));
    }

    public static class RatingRequest {
        public Integer rating;
        public String comment;
    }

    // Ratings & Unified Comments
    @PostMapping("/routes/{routeId}/rate")
    public ResponseEntity<?> rateRoute(Principal principal,
            @PathVariable Integer routeId,
            @RequestBody RatingRequest req) {
        return ResponseEntity
                .ok(interactionService.addRouteRating(principal.getName(), routeId, req.rating, req.comment));
    }
}
