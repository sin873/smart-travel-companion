package com.yizhaoqi.smartpai.service.travel;

import com.yizhaoqi.smartpai.entity.travel.TravelItinerary;
import com.yizhaoqi.smartpai.entity.travel.TravelItineraryDay;
import com.yizhaoqi.smartpai.entity.travel.TravelItineraryItem;
import com.yizhaoqi.smartpai.model.travel.ItineraryDTO;
import com.yizhaoqi.smartpai.model.travel.ItineraryDayDTO;
import com.yizhaoqi.smartpai.model.travel.ItineraryItemDTO;
import com.yizhaoqi.smartpai.repository.travel.TravelItineraryDayRepository;
import com.yizhaoqi.smartpai.repository.travel.TravelItineraryItemRepository;
import com.yizhaoqi.smartpai.repository.travel.TravelItineraryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ItineraryService {

    private static final Logger logger = LoggerFactory.getLogger(ItineraryService.class);

    @Autowired
    private TravelItineraryRepository travelItineraryRepository;

    @Autowired
    private TravelItineraryDayRepository travelItineraryDayRepository;

    @Autowired
    private TravelItineraryItemRepository travelItineraryItemRepository;

    /**
     * 保存行程
     * 保存 travel_itinerary 主表、travel_itinerary_day、travel_itinerary_item
     */
    @Transactional
    public String saveItinerary(ItineraryDTO itineraryDTO, String userId) {
        logger.info("Saving itinerary - userId: {}, title: {}", userId, itineraryDTO.getTitle());

        // 1. 生成 itineraryId
        String itineraryId = "itinerary_" + UUID.randomUUID().toString().substring(0, 8);

        // 2. 保存行程主表
        TravelItinerary itinerary = new TravelItinerary();
        itinerary.setItineraryId(itineraryId);
        itinerary.setUserId(userId);
        itinerary.setTitle(itineraryDTO.getTitle() != null ? itineraryDTO.getTitle() : 
                          itineraryDTO.getDestination() + " 行程");
        itinerary.setDestination(itineraryDTO.getDestination());
        itinerary.setStartDate(itineraryDTO.getStartDate());
        itinerary.setEndDate(itineraryDTO.getEndDate());
        itinerary.setTravelerCount(itineraryDTO.getTravelerCount() != null ? itineraryDTO.getTravelerCount() : 1);
        itinerary.setBudget(itineraryDTO.getBudget());
        itinerary.setStatus(itineraryDTO.getStatus() != null ? itineraryDTO.getStatus() : "PLANNED");
        itinerary.setSummary(itineraryDTO.getSummary());
        travelItineraryRepository.save(itinerary);

        // 3. 保存每日行程和景点
        if (itineraryDTO.getDays() != null && !itineraryDTO.getDays().isEmpty()) {
            for (ItineraryDayDTO dayDTO : itineraryDTO.getDays()) {
                saveItineraryDay(dayDTO, itineraryId);
            }
        }

        logger.info("Itinerary saved successfully - itineraryId: {}", itineraryId);
        return itineraryId;
    }

    /**
     * 获取用户行程列表（精简字段）
     */
    public List<ItineraryDTO> getUserItineraries(String userId) {
        logger.info("Getting user itineraries - userId: {}", userId);

        List<TravelItinerary> itineraries = travelItineraryRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<ItineraryDTO> result = new ArrayList<>();

        for (TravelItinerary itinerary : itineraries) {
            ItineraryDTO dto = new ItineraryDTO();
            dto.setItineraryId(itinerary.getItineraryId());
            dto.setTitle(itinerary.getTitle());
            dto.setDestination(itinerary.getDestination());
            dto.setStartDate(itinerary.getStartDate());
            dto.setEndDate(itinerary.getEndDate());
            dto.setTravelerCount(itinerary.getTravelerCount());
            dto.setBudget(itinerary.getBudget());
            dto.setStatus(itinerary.getStatus());
            // 列表页不返回 days
            dto.setDays(null);
            result.add(dto);
        }

        return result;
    }

    /**
     * 获取行程详情（完整信息）
     */
    public ItineraryDTO getItineraryDetail(String itineraryId) {
        logger.info("Getting itinerary detail - itineraryId: {}", itineraryId);

        TravelItinerary itinerary = travelItineraryRepository.findByItineraryId(itineraryId)
                .orElseThrow(() -> new RuntimeException("Itinerary not found: " + itineraryId));

        // 转换主表
        ItineraryDTO dto = new ItineraryDTO();
        dto.setItineraryId(itinerary.getItineraryId());
        dto.setTitle(itinerary.getTitle());
        dto.setDestination(itinerary.getDestination());
        dto.setStartDate(itinerary.getStartDate());
        dto.setEndDate(itinerary.getEndDate());
        dto.setTravelerCount(itinerary.getTravelerCount());
        dto.setBudget(itinerary.getBudget());
        dto.setStatus(itinerary.getStatus());
        dto.setSummary(itinerary.getSummary());

        // 加载每日行程
        List<TravelItineraryDay> days = travelItineraryDayRepository.findByItineraryIdOrderByDayNumberAsc(itineraryId);
        List<ItineraryDayDTO> dayDTOs = new ArrayList<>();

        for (TravelItineraryDay day : days) {
            ItineraryDayDTO dayDTO = convertToDayDTO(day);
            dayDTOs.add(dayDTO);
        }

        dto.setDays(dayDTOs);
        return dto;
    }

    /**
     * 删除行程（物理删除）
     * TODO: 后续可考虑改为软删除
     */
    @Transactional
    public void deleteItinerary(String itineraryId, String userId) {
        logger.info("Deleting itinerary - userId: {}, itineraryId: {}", userId, itineraryId);

        // 1. 验证行程存在且属于该用户
        TravelItinerary itinerary = travelItineraryRepository.findByItineraryId(itineraryId)
                .orElseThrow(() -> new RuntimeException("Itinerary not found: " + itineraryId));

        if (!itinerary.getUserId().equals(userId)) {
            throw new RuntimeException("No permission to delete this itinerary");
        }

        // 2. 删除关联的 items
        List<TravelItineraryDay> days = travelItineraryDayRepository.findByItineraryIdOrderByDayNumberAsc(itineraryId);
        for (TravelItineraryDay day : days) {
            List<TravelItineraryItem> items = travelItineraryItemRepository.findByDayIdOrderByOrderIndexAsc(day.getDayId());
            travelItineraryItemRepository.deleteAll(items);
        }

        // 3. 删除关联的 days
        travelItineraryDayRepository.deleteAll(days);

        // 4. 删除主表
        travelItineraryRepository.delete(itinerary);

        logger.info("Itinerary deleted successfully - itineraryId: {}", itineraryId);
    }

    // ==================== 私有方法 ====================

    /**
     * 保存单日行程及其景点
     */
    private void saveItineraryDay(ItineraryDayDTO dayDTO, String itineraryId) {
        String dayId = "day_" + UUID.randomUUID().toString().substring(0, 8);

        TravelItineraryDay day = new TravelItineraryDay();
        day.setDayId(dayId);
        day.setItineraryId(itineraryId);
        day.setDayNumber(dayDTO.getDayNumber());
        day.setTravelDate(dayDTO.getTravelDate());
        day.setTitle(dayDTO.getTitle());
        day.setNotes(dayDTO.getNotes());
        travelItineraryDayRepository.save(day);

        // 保存景点
        if (dayDTO.getItems() != null && !dayDTO.getItems().isEmpty()) {
            int orderIndex = 0;
            for (ItineraryItemDTO itemDTO : dayDTO.getItems()) {
                saveItineraryItem(itemDTO, dayId, orderIndex++);
            }
        }
    }

    /**
     * 保存单个景点
     */
    private void saveItineraryItem(ItineraryItemDTO itemDTO, String dayId, int orderIndex) {
        String itemId = "item_" + UUID.randomUUID().toString().substring(0, 8);

        TravelItineraryItem item = new TravelItineraryItem();
        item.setItemId(itemId);
        item.setDayId(dayId);
        item.setAttractionId(itemDTO.getAttractionId());
        item.setAttractionName(itemDTO.getAttractionName());
        item.setAddress(itemDTO.getAddress());
        item.setLatitude(itemDTO.getLatitude());
        item.setLongitude(itemDTO.getLongitude());
        item.setStartTime(itemDTO.getStartTime());
        item.setEndTime(itemDTO.getEndTime());
        item.setDurationMinutes(itemDTO.getDurationMinutes());
        item.setOrderIndex(itemDTO.getOrderIndex() != null ? itemDTO.getOrderIndex() : orderIndex);
        item.setNotes(itemDTO.getNotes());
        travelItineraryItemRepository.save(item);
    }

    /**
     * 将 TravelItineraryDay 转换为 ItineraryDayDTO
     */
    private ItineraryDayDTO convertToDayDTO(TravelItineraryDay day) {
        ItineraryDayDTO dayDTO = new ItineraryDayDTO();
        dayDTO.setDayId(day.getDayId());
        dayDTO.setDayNumber(day.getDayNumber());
        dayDTO.setTravelDate(day.getTravelDate());
        dayDTO.setTitle(day.getTitle());
        dayDTO.setNotes(day.getNotes());

        // 加载景点
        List<TravelItineraryItem> items = travelItineraryItemRepository.findByDayIdOrderByOrderIndexAsc(day.getDayId());
        List<ItineraryItemDTO> itemDTOs = new ArrayList<>();

        for (TravelItineraryItem item : items) {
            ItineraryItemDTO itemDTO = convertToItemDTO(item);
            itemDTOs.add(itemDTO);
        }

        dayDTO.setItems(itemDTOs);
        return dayDTO;
    }

    /**
     * 将 TravelItineraryItem 转换为 ItineraryItemDTO
     */
    private ItineraryItemDTO convertToItemDTO(TravelItineraryItem item) {
        ItineraryItemDTO itemDTO = new ItineraryItemDTO();
        itemDTO.setItemId(item.getItemId());
        itemDTO.setAttractionId(item.getAttractionId());
        itemDTO.setAttractionName(item.getAttractionName());
        itemDTO.setAddress(item.getAddress());
        itemDTO.setLatitude(item.getLatitude());
        itemDTO.setLongitude(item.getLongitude());
        itemDTO.setStartTime(item.getStartTime());
        itemDTO.setEndTime(item.getEndTime());
        itemDTO.setDurationMinutes(item.getDurationMinutes());
        itemDTO.setOrderIndex(item.getOrderIndex());
        itemDTO.setNotes(item.getNotes());
        return itemDTO;
    }
}
