package com.yizhaoqi.smartpai.repository.miniprogram;

import com.yizhaoqi.smartpai.entity.miniprogram.MiniprogramUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MiniprogramUserRepository extends JpaRepository<MiniprogramUser, Long> {
    
    Optional<MiniprogramUser> findByUserId(String userId);
    
    Optional<MiniprogramUser> findByOpenid(String openid);
    
    Optional<MiniprogramUser> findByUnionid(String unionid);
}
