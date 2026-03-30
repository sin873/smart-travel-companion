package com.yizhaoqi.smartpai.service.miniprogram;

import com.yizhaoqi.smartpai.entity.miniprogram.MiniprogramUser;
import com.yizhaoqi.smartpai.model.miniprogram.WxLoginRequest;
import com.yizhaoqi.smartpai.model.miniprogram.WxLoginResponse;
import com.yizhaoqi.smartpai.repository.miniprogram.MiniprogramUserRepository;
import com.yizhaoqi.smartpai.utils.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class MiniprogramAuthService {

    private static final Logger logger = LoggerFactory.getLogger(MiniprogramAuthService.class);

    @Autowired
    private MiniprogramUserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Value("${wechat.miniprogram.appId:}")
    private String appId;

    @Value("${wechat.miniprogram.appSecret:}")
    private String appSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 微信小程序登录
     */
    @Transactional
    public WxLoginResponse wxLogin(WxLoginRequest request) {
        String code = request.getCode();
        
        // 调用微信API获取openid
        Map<String, String> wxResult = code2Session(code);
        String openid = wxResult.get("openid");
        String unionid = wxResult.get("unionid");

        if (openid == null || openid.isEmpty()) {
            throw new RuntimeException("Failed to get openid from WeChat");
        }

        // 查找用户
        Optional<MiniprogramUser> existingUser = userRepository.findByOpenid(openid);
        boolean isNewUser = !existingUser.isPresent();

        MiniprogramUser user;
        if (isNewUser) {
            // 创建新用户
            user = new MiniprogramUser();
            user.setUserId("user_" + UUID.randomUUID().toString().substring(0, 8));
            user.setOpenid(openid);
            user.setUnionid(unionid);
            
            if (request.getUserInfo() != null) {
                user.setNickName(request.getUserInfo().getNickName());
                user.setAvatarUrl(request.getUserInfo().getAvatarUrl());
            }
            
            userRepository.save(user);
            logger.info("New miniprogram user registered: {}", user.getUserId());
        } else {
            user = existingUser.get();
            
            // 更新用户信息
            if (request.getUserInfo() != null) {
                user.setNickName(request.getUserInfo().getNickName());
                user.setAvatarUrl(request.getUserInfo().getAvatarUrl());
                userRepository.save(user);
            }
        }

        // 生成JWT token
        String token = jwtUtils.generateToken(user.getUserId());

        WxLoginResponse response = new WxLoginResponse();
        response.setToken(token);
        response.setUserId(user.getUserId());
        response.setIsNewUser(isNewUser);

        return response;
    }

    /**
     * 调用微信code2Session接口
     */
    private Map<String, String> code2Session(String code) {
        // 模拟微信API响应（实际应该调用真实微信API）
        Map<String, String> result = new HashMap<>();
        
        // 为了演示,直接返回模拟的openid
        // 实际项目中应该调用: https://api.weixin.qq.com/sns/jscode2session
        result.put("openid", "mock_openid_" + code.hashCode());
        result.put("unionid", "mock_unionid_" + code.hashCode());
        result.put("session_key", "mock_session_key");

        logger.info("Mock WeChat code2Session for code: {}", code);
        return result;
    }
}
