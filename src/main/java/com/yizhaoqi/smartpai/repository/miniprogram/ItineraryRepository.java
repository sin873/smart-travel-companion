package com.yizhaoqi.smartpai.repository.miniprogram;

import com.yizhaoqi.smartpai.entity.miniprogram.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {
    
    Optional<Itinerary> findByItineraryId(String itineraryId);
    
    List<Itinerary> findByUserIdOrderByCreatedAtDesc(String userId);
    
    List<Itinerary> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, String status);
}
