package com.lelcc.utils;

import cn.hutool.core.convert.Convert;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import com.lelcc.common.BusinessException;
import com.lelcc.common.LoginUser;
import com.lelcc.common.UserType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtils {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_USER_TYPE = "userType";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    public String createToken(Long userId, UserType userType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_USER_TYPE, userType.name());
        claims.put("exp", new Date(System.currentTimeMillis() + expiration));
        return JWTUtil.createToken(claims, secret.getBytes());
    }

    /**
     * 解析并校验令牌，任意一步不通过都抛 401。
     */
    public LoginUser parseToken(String token) {
        try {
            JWT jwt = JWTUtil.parseToken(token).setKey(secret.getBytes());
            // 实测 JWTUtil.verify() 只校验签名、不校验 exp（过期令牌仍返回 true），
            // 必须用 validate() 才能同时拦住过期令牌，切勿改回 verify()。
            if (!jwt.validate(0)) {
                throw new BusinessException(401, "登录状态已失效，请重新登录");
            }
            Object userId = jwt.getPayload(CLAIM_USER_ID);
            Object userType = jwt.getPayload(CLAIM_USER_TYPE);
            if (userId == null || userType == null) {
                throw new BusinessException(401, "登录状态已失效，请重新登录");
            }
            return new LoginUser(Convert.toLong(userId), UserType.valueOf(String.valueOf(userType)));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(401, "登录状态已失效，请重新登录");
        }
    }
}
