package com.yizhaoqi.smartpai.repository.miniprogram;

import com.yizhaoqi.smartpai.entity.miniprogram.ItineraryAttraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItineraryAttractionRepository extends JpaRepository<ItineraryAttraction, Long> {
    
    Optional<ItineraryAttraction> findByItemId(String itemId);
    
    List<ItineraryAttraction> findByDayIdOrderByOrderIndexAsc(String dayId);
}
