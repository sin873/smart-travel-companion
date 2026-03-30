package com.yizhaoqi.smartpai.repository.travel;

import com.yizhaoqi.smartpai.entity.travel.TravelItineraryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TravelItineraryItemRepository extends JpaRepository<TravelItineraryItem, Long> {

    Optional<TravelItineraryItem> findByItemId(String itemId);

    List<TravelItineraryItem> findByDayIdOrderByOrderIndexAsc(String dayId);
}
