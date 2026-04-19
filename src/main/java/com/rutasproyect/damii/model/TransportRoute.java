package com.rutasproyect.damii.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transport_routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransportRoute {
    @Id
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(name = "route_ref")
    private String routeRef;

    private String network;

    @Column(name = "is_verified")
    private Boolean isVerified = false;

    @Column(name = "risk_level")
    private Double riskLevel = 1.0;
}