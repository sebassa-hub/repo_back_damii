package com.rutasproyect.damii.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.rutasproyect.damii.model.Report;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Integer> {

    // Busca reportes recientes (últimas 24h) en un radio de metros
    @Query(value = """
            SELECT * FROM reports r
            WHERE ST_Distance_Sphere(r.location, POINT(:userLng, :userLat)) <= :radiusMeters
            AND r.report_time >= NOW() - INTERVAL 24 HOUR
            """, nativeQuery = true)
    List<Report> findRecentIncidentsNearLocation(
            @Param("userLat") Double userLat,
            @Param("userLng") Double userLng,
            @Param("radiusMeters") Double radiusMeters);
}