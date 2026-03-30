package com.yizhaoqi.smartpai.model.miniprogram;

import lombok.Data;

@Data
public class WxLoginRequest {
    private String code;
    private UserInfo userInfo;

    @Data
    public static class UserInfo {
        private String nickName;
        private String avatarUrl;
    }
}
