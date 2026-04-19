package com.rutasproyect.damii.dto;

public record RouteSummaryDTO(
        Integer id,
        String name,
        String routeRef,
        String network,
        Boolean isVerified,
        Double riskLevel) {
}