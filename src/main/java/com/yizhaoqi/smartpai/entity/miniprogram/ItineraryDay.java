package com.yizhaoqi.smartpai.entity.miniprogram;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "itinerary_day")
public class ItineraryDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "day_id", unique = true, nullable = false, length = 64)
    private String dayId;

    @Column(name = "itinerary_id", nullable = false, length = 64)
    private String itineraryId;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "title", length = 256)
    private String title;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
