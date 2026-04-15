package com.yizhaoqi.smartpai.service.travel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yizhaoqi.smartpai.model.travel.AmapPoiSearchResult;
import com.yizhaoqi.smartpai.model.travel.AmapRoutePlanResult;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AmapServiceTest {

    private OkHttpClient httpClient;
    private AmapService amapService;

    @BeforeEach
    void setUp() {
        httpClient = mock(OkHttpClient.class);
        amapService = new AmapService(
                mock(RestTemplate.class),
                new ObjectMapper(),
                "test-key",
                "https://restapi.amap.com/v3/place/text",
                "https://restapi.amap.com/v3/direction/driving",
                httpClient
        );
    }

    @Test
    void shouldParseScenicPoiSearchResult() throws Exception {
        mockSingleResponse("""
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
    void shouldDeduplicateAndPreferTourScenicPois() throws Exception {
        mockSingleResponse("""
                {
                  "status":"1",
                  "info":"OK",
                  "infocode":"10000",
                  "pois":[
                    {
                      "id":"A001",
                      "name":"杭州西湖风景名胜区",
                      "address":"龙井路1号",
                      "cityname":"杭州",
                      "adname":"西湖区",
                      "type":"风景名胜;国家级景点",
                      "location":"120.121358,30.222692"
                    },
                    {
                      "id":"A001",
                      "name":"杭州西湖风景名胜区",
                      "address":"龙井路1号",
                      "cityname":"杭州",
                      "adname":"西湖区",
                      "type":"风景名胜;国家级景点",
                      "location":"120.121358,30.222692"
                    },
                    {
                      "id":"B002",
                      "name":"河坊街",
                      "address":"河坊街180号",
                      "cityname":"杭州",
                      "adname":"上城区",
                      "type":"购物服务;特色商业街;步行街",
                      "location":"120.168923,30.240073"
                    }
                  ]
                }
                """);

        AmapPoiSearchResult result = amapService.searchPoiResult("杭州", "景点", null, 10);

        assertEquals(2, result.getPois().size());
        assertEquals("杭州西湖风景名胜区", result.getPois().get(0).getName());
    }

    @Test
    void shouldPlanDrivingRouteAndReturnStructuredSummary() throws Exception {
        Call call1 = mockCall("""
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
                """);
        Call call2 = mockCall("""
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
                """);
        Call call3 = mockCall("""
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
        when(httpClient.newCall(any(Request.class))).thenReturn(call1, call2, call3);

        AmapRoutePlanResult result = amapService.planDrivingRouteResult("杭州", "西湖", "灵隐寺");

        assertTrue(result.isSuccess());
        assertEquals("7.2公里", result.getDistanceText());
        assertEquals("18分钟", result.getDurationText());
        assertEquals("速度优先", result.getStrategy());
        assertTrue(result.getPolyline().contains("120.1,30.2"));
        assertTrue(amapService.summarizeRoute(result).contains("驾车约18分钟"));
    }

    @Test
    void shouldFallbackWhenAmapCallFails() throws Exception {
        Call call = mock(Call.class);
        when(httpClient.newCall(any(Request.class))).thenReturn(call);
        when(call.execute()).thenThrow(new IOException("network error"));

        AmapPoiSearchResult poiResult = amapService.searchScenicPois("杭州");
        AmapRoutePlanResult routeResult = amapService.planDrivingRouteResult("杭州", "西湖", "灵隐寺");

        assertFalse(poiResult.isSuccess());
        assertEquals(List.of(), poiResult.getPois());
        assertTrue(poiResult.getMessage().contains("降级"));
        assertFalse(routeResult.isSuccess());
        assertTrue(routeResult.getMessage().contains("未能在杭州找到可用于路线规划的地点")
                || routeResult.getMessage().contains("降级"));
    }

    private void mockSingleResponse(String body) throws Exception {
        Call call = mockCall(body);
        when(httpClient.newCall(any(Request.class))).thenReturn(call);
    }

    private Call mockCall(String body) throws Exception {
        Call call = mock(Call.class);
        when(call.execute()).thenReturn(new Response.Builder()
                .request(new Request.Builder().url("https://example.com").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(body, MediaType.get("application/json; charset=utf-8")))
                .build());
        return call;
    }
}
