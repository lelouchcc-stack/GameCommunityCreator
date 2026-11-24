package com.gcc.community.security;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.gcc.community.domain.AppUser;
import com.gcc.community.repository.AppUserRepository;
import com.gcc.community.repository.CommunityProjectRepository;

// 启动时创建初始开发者账号，顺便认领没有归属的旧项目。
@Component
public class BootstrapAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAccountInitializer.class);

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CommunityProjectRepository communityProjectRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.username:}")
    private String username;

    @Value("${app.bootstrap.password:}")
    private String password;

    @Value("${app.bootstrap.display-name:独立开发者}")
    private String displayName;

    @Override
    public void run(ApplicationArguments args) {
        String configuredUsername = username == null ? "" : username.strip();
        if (configuredUsername.isEmpty()) {
            log.warn("未配置 APP_BOOTSTRAP_USERNAME：跳过初始开发者创建与旧项目认领（可访问 /api/auth/register 注册）");
            return;
        }
        Optional<AppUser> existing = appUserRepository.findByUsername(configuredUsername);
        Long ownerId;
        if (existing.isPresent()) {
            ownerId = existing.get().id();
        } else if (appUserRepository.countAll() == 0) {
            ownerId = createBootstrapUser(configuredUsername);
            if (ownerId == null) {
                return;
            }
        } else {
            log.warn("已存在其他开发者账号且 {} 不存在：跳过创建与旧项目认领（避免接管他人项目）", configuredUsername);
            return;
        }
        int claimed = communityProjectRepository.claimUnownedProjects(ownerId);
        if (claimed > 0) {
            log.info("已将 {} 个无主旧项目认领给初始开发者 {}（不清库、不改页面/内容）", claimed, configuredUsername);
        }
    }

    private Long createBootstrapUser(String configuredUsername) {
        if (password == null || password.isBlank()) {
            log.warn("已配置 APP_BOOTSTRAP_USERNAME 但缺少 APP_BOOTSTRAP_PASSWORD：跳过初始开发者创建（源码不内置默认口令）");
            return null;
        }
        if (!CredentialRules.isValidUsername(configuredUsername)) {
            log.warn("APP_BOOTSTRAP_USERNAME 不符合用户名规则（3-30 位字母/数字/下划线/中划线）：跳过初始开发者创建");
            return null;
        }
        if (!CredentialRules.isStrongPassword(password)) {
            log.warn("APP_BOOTSTRAP_PASSWORD 不符合密码规则（≥8 位且同时含字母与数字）：跳过初始开发者创建");
            return null;
        }
        String name = displayName == null || displayName.isBlank() ? "独立开发者" : displayName.strip();
        if (name.length() > 60) {
            name = name.substring(0, 60);
        }
        long id = appUserRepository.insert(configuredUsername, passwordEncoder.encode(password), name,
                LocalDateTime.now());
        log.info("已创建初始开发者账号 {}（密码来自环境变量，库中只存 BCrypt 哈希）", configuredUsername);
        return id;
    }
}
