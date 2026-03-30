package com.yizhaoqi.smartpai.repository.travel;

import com.yizhaoqi.smartpai.entity.travel.TravelItinerary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TravelItineraryRepository extends JpaRepository<TravelItinerary, Long> {

    Optional<TravelItinerary> findByItineraryId(String itineraryId);

    List<TravelItinerary> findByUserIdOrderByCreatedAtDesc(String userId);

    List<TravelItinerary> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, String status);
}
