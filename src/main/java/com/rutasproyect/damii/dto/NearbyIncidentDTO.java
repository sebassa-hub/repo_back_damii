package com.rutasproyect.damii.dto;

public record NearbyIncidentDTO(
        String reportType, // "Robo", "Acoso"
        Double latitude,
        Double longitude,
        String timeAgo // Ej: "Hace 2 horas" (calculado en el Service)
) {
}