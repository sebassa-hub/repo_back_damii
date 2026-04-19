package com.rutasproyect.damii.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "route_risk_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RouteRiskScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private TransportRoute route;

    @Column(name = "risk_day")
    private Double riskDay;

    @Column(name = "risk_night")
    private Double riskNight;

    @Column(name = "last_updated", updatable = false)
    private LocalDateTime lastUpdated = LocalDateTime.now();

    // Con @PreUpdate aseguramos que la fecha cambie sola cada vez que el cronjob
    // actualiza el score
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}