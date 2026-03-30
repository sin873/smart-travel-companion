package com.yizhaoqi.smartpai.entity.miniprogram;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "itinerary_attraction")
public class ItineraryAttraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", unique = true, nullable = false, length = 64)
    private String itemId;

    @Column(name = "day_id", nullable = false, length = 64)
    private String dayId;

    @Column(name = "attraction_id", length = 64)
    private String attractionId;

    @Column(name = "attraction_name", nullable = false, length = 256)
    private String attractionName;

    @Column(name = "address", length = 512)
    private String address;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
