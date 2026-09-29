package com.lelcc.interceptor;

import com.lelcc.common.BusinessException;
import com.lelcc.common.LoginUser;
import com.lelcc.common.UserContext;
import com.lelcc.common.UserType;
import com.lelcc.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 双端鉴权拦截器。同一个类按身份域实例化两次：
 * 后台接口只接受 ADMIN 令牌，用户端接口只接受 MEMBER 令牌，令牌不可跨端使用。
 */
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtils jwtUtils;
    private final UserType requiredType;

    public AuthInterceptor(JwtUtils jwtUtils, UserType requiredType) {
        this.jwtUtils = jwtUtils;
        this.requiredType = requiredType;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(401, "未登录，请先登录");
        }

        LoginUser loginUser = jwtUtils.parseToken(authorization.substring(BEARER_PREFIX.length()));
        if (loginUser.type() != requiredType) {
            throw new BusinessException(401, "当前登录身份无权访问该接口");
        }

        UserContext.set(loginUser);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.remove();
    }
}
