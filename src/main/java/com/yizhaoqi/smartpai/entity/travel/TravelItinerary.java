package com.yizhaoqi.smartpai.entity.travel;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "travel_itinerary")
public class TravelItinerary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "itinerary_id", unique = true, nullable = false, length = 64)
    private String itineraryId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "title", nullable = false, length = 256)
    private String title;

    @Column(name = "destination", nullable = false, length = 128)
    private String destination;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "traveler_count")
    private Integer travelerCount = 1;

    @Column(name = "budget", precision = 10, scale = 2)
    private BigDecimal budget;

    @Column(name = "status", length = 32)
    private String status = "DRAFT";

    @Column(name = "summary", columnDefinition = "text")
    private String summary;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
