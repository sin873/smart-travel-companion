package com.yizhaoqi.smartpai.repository.travel;

import com.yizhaoqi.smartpai.entity.travel.TravelItineraryDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TravelItineraryDayRepository extends JpaRepository<TravelItineraryDay, Long> {

    Optional<TravelItineraryDay> findByDayId(String dayId);

    List<TravelItineraryDay> findByItineraryIdOrderByDayNumberAsc(String itineraryId);
}
