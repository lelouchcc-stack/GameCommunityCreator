package com.gcc.community.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gcc.community.common.ApiException;
import com.gcc.community.domain.AppUser;
import com.gcc.community.dto.UserView;
import com.gcc.community.repository.AppUserRepository;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;

// 取当前登录用户，没登录就报 401。
@Component
public class CurrentUserProvider {

    public static final String UNAUTHORIZED_MESSAGE = "登录已过期或未登录，请重新登录";

    @Autowired
    private AppUserRepository appUserRepository;

    // 当前登录用户 id，没登录就抛 401
    public long requireUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (NotLoginException ex) {
            throw unauthorized();
        }
    }

    // 当前登录用户的信息，没登录就抛 401
    public UserView requireView() {
        AppUser user = appUserRepository.findById(requireUserId())
                .orElseThrow(this::unauthorized);
        return new UserView(user.id(), user.username(), user.displayName());
    }

    // 判断当前用户是不是指定的人
    public boolean isCurrentUser(long userId) {
        try {
            return StpUtil.getLoginIdAsLong() == userId;
        } catch (NotLoginException ex) {
            return false;
        }
    }

    private ApiException unauthorized() {
        return new ApiException(401, UNAUTHORIZED_MESSAGE);
    }
}
