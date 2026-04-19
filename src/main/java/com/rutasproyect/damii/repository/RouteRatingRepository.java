package com.rutasproyect.damii.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rutasproyect.damii.model.RouteRating;

@Repository
public interface RouteRatingRepository extends JpaRepository<RouteRating, Integer> {
}
