package com.yizhaoqi.smartpai.model.miniprogram;

import lombok.Data;

@Data
public class WxLoginResponse {
    private String token;
    private String userId;
    private Boolean isNewUser;
}
