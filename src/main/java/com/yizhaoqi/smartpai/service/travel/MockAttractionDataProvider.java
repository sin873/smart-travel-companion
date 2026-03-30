package com.yizhaoqi.smartpai.service.travel;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock 景点数据提供者
 * 当前为 Mock 实现，后续可替换为高德 MCP 获取真实景点数据
 */
public class MockAttractionDataProvider {

    // 城市 -> 景点列表 的映射
    private static final Map<String, List<MockAttraction>> CITY_ATTRACTIONS = new HashMap<>();

    static {
        // 北京景点
        CITY_ATTRACTIONS.put("北京", Arrays.asList(
                new MockAttraction("gugong", "故宫博物院", "北京市东城区景山前街4号",
                        39.9163, 116.3972, 180, Arrays.asList("historical", "cultural")),
                new MockAttraction("tiananmen", "天安门广场", "北京市东城区",
                        39.9087, 116.3975, 60, Arrays.asList("historical", "cultural")),
                new MockAttraction("summerpalace", "颐和园", "北京市海淀区新建宫门路19号",
                        39.9999, 116.2766, 180, Arrays.asList("nature", "historical")),
                new MockAttraction("greatwall", "八达岭长城", "北京市延庆区八达岭镇",
                        40.3579, 116.0213, 240, Arrays.asList("historical", "nature")),
                new MockAttraction("wangfujing", "王府井大街", "北京市东城区王府井大街",
                        39.9146, 116.4108, 120, Arrays.asList("shopping", "food")),
                new MockAttraction("nanluoguxiang", "南锣鼓巷", "北京市东城区南锣鼓巷",
                        39.9368, 116.4038, 90, Arrays.asList("food", "cultural")),
                new MockAttraction("beihai", "北海公园", "北京市西城区文津街1号",
                        39.9255, 116.3897, 120, Arrays.asList("nature", "historical")),
                new MockAttraction("hutong", "什刹海胡同游", "北京市西城区什刹海",
                        39.9388, 116.3788, 120, Arrays.asList("cultural", "food"))
        ));

        // 上海景点
        CITY_ATTRACTIONS.put("上海", Arrays.asList(
                new MockAttraction("bund", "外滩", "上海市黄浦区中山东一路",
                        31.2397, 121.4998, 120, Arrays.asList("historical", "cultural")),
                new MockAttraction("yuyuan", "豫园", "上海市黄浦区安仁街132号",
                        31.2275, 121.4925, 120, Arrays.asList("historical", "food")),
                new MockAttraction("orientalpearl", "东方明珠塔", "上海市浦东新区世纪大道1号",
                        31.2397, 121.4998, 90, Arrays.asList("cultural", "shopping")),
                new MockAttraction("disney", "上海迪士尼乐园", "上海市浦东新区黄赵路310号",
                        31.1434, 121.6570, 480, Arrays.asList("cultural", "nature")),
                new MockAttraction("nanjingroad", "南京路步行街", "上海市黄浦区南京东路",
                        31.2375, 121.4787, 120, Arrays.asList("shopping", "food")),
                new MockAttraction("xintiandi", "新天地", "上海市黄浦区太仓路181弄",
                        31.2204, 121.4691, 90, Arrays.asList("food", "cultural")),
                new MockAttraction("jadebuddha", "玉佛寺", "上海市普陀区安远路170号",
                        31.2404, 121.4369, 60, Arrays.asList("historical", "cultural")),
                new MockAttraction("zhujiajiao", "朱家角古镇", "上海市青浦区朱家角镇",
                        31.1036, 121.0442, 180, Arrays.asList("historical", "food"))
        ));

        // 杭州景点
        CITY_ATTRACTIONS.put("杭州", Arrays.asList(
                new MockAttraction("westlake", "西湖", "浙江省杭州市西湖区",
                        30.2471, 120.1444, 240, Arrays.asList("nature", "historical")),
                new MockAttraction("lingyin", "灵隐寺", "浙江省杭州市西湖区灵隐路法云弄1号",
                        30.2388, 120.1013, 120, Arrays.asList("historical", "cultural")),
                new MockAttraction("hpfvillage", "河坊街", "浙江省杭州市上城区河坊街",
                        30.2463, 120.1679, 90, Arrays.asList("food", "shopping")),
                new MockAttraction("songcheng", "宋城", "浙江省杭州市西湖区之江路148号",
                        30.1995, 120.0773, 180, Arrays.asList("cultural", "historical")),
                new MockAttraction("xixi", "西溪湿地", "浙江省杭州市西湖区天目山路518号",
                        30.2654, 120.0465, 180, Arrays.asList("nature", "cultural")),
                new MockAttraction("yuefei", "岳王庙", "浙江省杭州市西湖区北山路80号",
                        30.2567, 120.1345, 60, Arrays.asList("historical", "cultural")),
                new MockAttraction("sudi", "苏堤春晓", "浙江省杭州市西湖区苏堤",
                        30.2397, 120.1418, 90, Arrays.asList("nature", "historical")),
                new MockAttraction("longjing", "龙井茶园", "浙江省杭州市西湖区龙井村",
                        30.2145, 120.1123, 120, Arrays.asList("nature", "food"))
        ));

        // 成都景点
        CITY_ATTRACTIONS.put("成都", Arrays.asList(
                new MockAttraction("kuanzhai", "宽窄巷子", "四川省成都市青羊区长顺上街127号",
                        30.6578, 104.0506, 120, Arrays.asList("food", "cultural")),
                new MockAttraction("jinli", "锦里古街", "四川省成都市武侯区武侯祠大街231号",
                        30.6456, 104.0534, 120, Arrays.asList("food", "historical")),
                new MockAttraction("wuhouci", "武侯祠", "四川省成都市武侯区武侯祠大街231号",
                        30.6467, 104.0523, 90, Arrays.asList("historical", "cultural")),
                new MockAttraction("dujiangyan", "都江堰", "四川省成都市都江堰市公园路",
                        30.9946, 103.6157, 240, Arrays.asList("historical", "nature")),
                new MockAttraction("panda", "成都大熊猫繁育研究基地", "四川省成都市成华区熊猫大道1375号",
                        30.7209, 104.1054, 180, Arrays.asList("nature", "cultural")),
                new MockAttraction("chunxi", "春熙路", "四川省成都市锦江区春熙路",
                        30.6589, 104.0702, 120, Arrays.asList("shopping", "food")),
                new MockAttraction("sands", "金沙遗址博物馆", "四川省成都市青羊区金沙遗址路2号",
                        30.6823, 104.0123, 90, Arrays.asList("historical", "cultural")),
                new MockAttraction("taikoo", "太古里", "四川省成都市锦江区中纱帽街8号",
                        30.6567, 104.0812, 90, Arrays.asList("shopping", "food"))
        ));
    }

    /**
     * 获取指定城市的景点列表
     */
    public static List<MockAttraction> getAttractionsByCity(String city) {
        return CITY_ATTRACTIONS.getOrDefault(city, List.of());
    }

    /**
     * 判断是否支持该城市
     */
    public static boolean isCitySupported(String city) {
        return CITY_ATTRACTIONS.containsKey(city);
    }

    /**
     * 获取支持的城市列表
     */
    public static List<String> getSupportedCities() {
        return List.copyOf(CITY_ATTRACTIONS.keySet());
    }

    /**
     * Mock 景点数据模型
     */
    public static class MockAttraction {
        private String attractionId;
        private String attractionName;
        private String address;
        private double latitude;
        private double longitude;
        private int durationMinutes;
        private List<String> categories;

        public MockAttraction(String attractionId, String attractionName, String address,
                               double latitude, double longitude, int durationMinutes,
                               List<String> categories) {
            this.attractionId = attractionId;
            this.attractionName = attractionName;
            this.address = address;
            this.latitude = latitude;
            this.longitude = longitude;
            this.durationMinutes = durationMinutes;
            this.categories = categories;
        }

        public String getAttractionId() { return attractionId; }
        public String getAttractionName() { return attractionName; }
        public String getAddress() { return address; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public int getDurationMinutes() { return durationMinutes; }
        public List<String> getCategories() { return categories; }

        public boolean hasCategory(String category) {
            return categories.contains(category);
        }
    }
}
