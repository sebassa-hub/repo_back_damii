package com.rutasproyect.damii.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rutasproyect.damii.model.RouteComment;

@Repository
public interface RouteCommentRepository extends JpaRepository<RouteComment, Integer> {
    List<RouteComment> findByRouteId(Integer routeId);
}
