package com.yizhaoqi.smartpai.repository.miniprogram;

import com.yizhaoqi.smartpai.entity.miniprogram.ItineraryDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItineraryDayRepository extends JpaRepository<ItineraryDay, Long> {
    
    Optional<ItineraryDay> findByDayId(String dayId);
    
    List<ItineraryDay> findByItineraryIdOrderByDayNumberAsc(String itineraryId);
}
