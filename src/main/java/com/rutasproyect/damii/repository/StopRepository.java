package com.rutasproyect.damii.repository;

import com.rutasproyect.damii.model.Stop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface StopRepository extends JpaRepository<Stop, Integer> {

    // Busca paraderos en un radio específico y los ordena del más cercano al más
    // lejano
    @Query(value = """
            SELECT * FROM stops s
            WHERE ST_Distance_Sphere(POINT(s.longitude, s.latitude), POINT(:userLng, :userLat)) <= :radiusMeters
            ORDER BY ST_Distance_Sphere(POINT(s.longitude, s.latitude), POINT(:userLng, :userLat)) ASC
            """, nativeQuery = true)
    List<Stop> findNearbyStops(
            @Param("userLat") Double userLat,
            @Param("userLng") Double userLng,
            @Param("radiusMeters") Double radiusMeters);

    Optional<Stop> findByLatitudeAndLongitude(Double latitude, Double longitude);
}
