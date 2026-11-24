package com.gcc.community.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gcc.community.common.ApiException;
import com.gcc.community.domain.AppUser;
import com.gcc.community.dto.AuthTokenView;
import com.gcc.community.dto.LoginRequest;
import com.gcc.community.dto.RegisterRequest;
import com.gcc.community.dto.UserView;
import com.gcc.community.repository.AppUserRepository;
import com.gcc.community.security.CredentialRules;
import com.gcc.community.security.CurrentUserProvider;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;

/** 开发者账号的注册、登录和退出。 */
@Service
public class AuthService {

    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private CurrentUserProvider currentUserProvider;

    // 注册，成功后直接登录
    @Transactional
    public AuthTokenView register(RegisterRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        String username = request.username() == null ? null : request.username().strip();
        String password = request.password();
        String displayName = request.displayName() == null ? null : request.displayName().strip();
        CredentialRules.validateRegistration(username, password, displayName, errors);
        if (!errors.isEmpty()) {
            Map.Entry<String, String> first = errors.entrySet().iterator().next();
            throw ApiException.badRequest("注册参数校验失败：" + first.getKey() + " " + first.getValue(),
                    errors);
        }
        if (appUserRepository.existsByUsername(username)) {
            throw usernameConflict();
        }
        long id;
        try {
            id = appUserRepository.insert(username, passwordEncoder.encode(password), displayName,
                    LocalDateTime.now());
        } catch (DuplicateKeyException ex) {
            // 并发注册同名用户，由唯一键兜底
            throw usernameConflict();
        }
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("注册后无法读取开发者账号"));
        return issueToken(user);
    }

    // 登录
    public AuthTokenView login(LoginRequest request) {
        String username = request.username() == null ? null : request.username().strip();
        AppUser user = username == null ? null : appUserRepository.findByUsername(username).orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new ApiException(401, "用户名或密码错误");
        }
        return issueToken(user);
    }

    // 退出登录
    public void logout() {
        try {
            StpUtil.logout();
        } catch (NotLoginException ex) {
            // token 已经失效或者没带，也当退出成功
        }
    }

    // 当前登录的用户
    public UserView me() {
        return currentUserProvider.requireView();
    }

    // 建登录态，返回 token 和用户信息
    private AuthTokenView issueToken(AppUser user) {
        StpUtil.login(user.id());
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        return new AuthTokenView(
                tokenInfo.getTokenValue(),
                "Bearer",
                tokenInfo.getTokenTimeout(),
                new UserView(user.id(), user.username(), user.displayName()));
    }

    private ApiException usernameConflict() {
        return new ApiException(409, "用户名已被占用", Map.of("username", "用户名已被占用"));
    }
}
