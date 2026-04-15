package com.yizhaoqi.smartpai.service.travel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yizhaoqi.smartpai.model.travel.AmapPoiSearchResult;
import com.yizhaoqi.smartpai.model.travel.AmapRoutePlanResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AmapServiceTest {

    private RestTemplate restTemplate;
    private AmapService amapService;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        amapService = new AmapService(
                restTemplate,
                new ObjectMapper(),
                "test-key",
                "https://restapi.amap.com/v3/place/text",
                "https://restapi.amap.com/v3/direction/driving"
        );
    }

    @Test
    void shouldParseScenicPoiSearchResult() {
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("""
                {
                  "status":"1",
                  "info":"OK",
                  "infocode":"10000",
                  "pois":[
                    {
                      "id":"B001",
                      "name":"西湖风景区",
                      "address":"龙井路1号",
                      "cityname":"杭州",
                      "adname":"西湖区",
                      "type":"风景名胜;景点",
                      "location":"120.153576,30.243173"
                    }
                  ]
                }
                """);

        AmapPoiSearchResult result = amapService.searchScenicPois("杭州");

        assertTrue(result.isSuccess());
        assertEquals(1, result.getPois().size());
        assertEquals("西湖风景区", result.getPois().get(0).getName());
        assertEquals("杭州", result.getPois().get(0).getCity());
        assertEquals("西湖区", result.getPois().get(0).getDistrict());
        assertTrue(amapService.summarizePoiSearch(result).contains("坐标"));
    }

    @Test
    void shouldPlanDrivingRouteAndReturnStructuredSummary() {
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("""
                        {
                          "status":"1",
                          "info":"OK",
                          "infocode":"10000",
                          "pois":[
                            {
                              "id":"O001",
                              "name":"西湖风景区",
                              "address":"龙井路1号",
                              "cityname":"杭州",
                              "adname":"西湖区",
                              "type":"风景名胜",
                              "location":"120.153576,30.243173"
                            }
                          ]
                        }
                        """)
                .thenReturn("""
                        {
                          "status":"1",
                          "info":"OK",
                          "infocode":"10000",
                          "pois":[
                            {
                              "id":"D001",
                              "name":"灵隐寺",
                              "address":"法云弄1号",
                              "cityname":"杭州",
                              "adname":"西湖区",
                              "type":"风景名胜",
                              "location":"120.101398,30.240018"
                            }
                          ]
                        }
                        """)
                .thenReturn("""
                        {
                          "status":"1",
                          "info":"OK",
                          "infocode":"10000",
                          "route":{
                            "paths":[
                              {
                                "distance":"7200",
                                "duration":"1080",
                                "strategy":"速度优先",
                                "steps":[
                                  { "polyline":"120.1,30.2;120.2,30.3" }
                                ]
                              }
                            ]
                          }
                        }
                        """);

        AmapRoutePlanResult result = amapService.planDrivingRouteResult("杭州", "西湖", "灵隐寺");

        assertTrue(result.isSuccess());
        assertEquals("7.2公里", result.getDistanceText());
        assertEquals("18分钟", result.getDurationText());
        assertEquals("速度优先", result.getStrategy());
        assertTrue(result.getPolyline().contains("120.1,30.2"));
        assertTrue(amapService.summarizeRoute(result).contains("驾车约18分钟"));
    }

    @Test
    void shouldFallbackWhenAmapCallFails() {
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenThrow(new RuntimeException("network error"));

        AmapPoiSearchResult poiResult = amapService.searchScenicPois("杭州");
        AmapRoutePlanResult routeResult = amapService.planDrivingRouteResult("杭州", "西湖", "灵隐寺");

        assertFalse(poiResult.isSuccess());
        assertEquals(List.of(), poiResult.getPois());
        assertTrue(poiResult.getMessage().contains("降级"));
        assertFalse(routeResult.isSuccess());
        assertTrue(routeResult.getMessage().contains("未能在杭州找到可用于路线规划的地点")
                || routeResult.getMessage().contains("降级"));
    }
}
