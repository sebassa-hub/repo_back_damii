package com.rutasproyect.damii.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rutasproyect.damii.model.FavoriteRoute;

@Repository
public interface FavoriteRouteRepository extends JpaRepository<FavoriteRoute, Integer> {
    List<FavoriteRoute> findByUserId(Integer userId);
    Optional<FavoriteRoute> findByUserIdAndRouteId(Integer userId, Integer routeId);
    boolean existsByUserIdAndRouteId(Integer userId, Integer routeId);
}
