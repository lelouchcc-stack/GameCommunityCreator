package com.lelcc.controller;

import com.lelcc.common.Result;
import com.lelcc.dto.MemberLoginDTO;
import com.lelcc.dto.MemberLoginVO;
import com.lelcc.dto.MemberRegisterDTO;
import com.lelcc.service.AppAuthService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 玩家登录/注册。
 * /app/login 与 /app/register 已在 WebMvcConfig 里对会员拦截器放行——匿名用户必须能调，
 * 因此这两个接口里不能出现任何需要登录态的逻辑。
 */
@RestController
@RequestMapping("/app")
public class AppAuthController {

    @Resource
    private AppAuthService appAuthService;

    @PostMapping("/login")
    public Result<MemberLoginVO> login(@Valid @RequestBody MemberLoginDTO dto) {
        return Result.success(appAuthService.login(dto));
    }

    @PostMapping("/register")
    public Result<MemberLoginVO> register(@Valid @RequestBody MemberRegisterDTO dto) {
        return Result.success(appAuthService.register(dto));
    }
}
