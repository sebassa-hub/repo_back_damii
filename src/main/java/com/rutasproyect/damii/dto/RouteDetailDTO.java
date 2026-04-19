package com.rutasproyect.damii.dto;

import java.util.List;

public record RouteDetailDTO(
        Integer id,
        String name,
        Double riskLevel,
        List<RouteCoordinateDTO> path // Lista ordenada para MapPolyline
) {
}