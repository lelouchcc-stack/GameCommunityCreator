package com.lelcc.controller;

import com.lelcc.common.Result;
import com.lelcc.dto.MemberInfoVO;
import com.lelcc.service.AppAuthService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 玩家个人相关接口。当前只提供「当前登录人信息」，
 * 用户端启动时用它校验令牌并恢复登录态；其余（我的帖子/收藏/进度）随各自模块（03-12）接入。
 */
@RestController
@RequestMapping("/app/member")
public class AppMemberController {

    @Resource
    private AppAuthService appAuthService;

    @GetMapping("/me")
    public Result<MemberInfoVO> me() {
        return Result.success(appAuthService.currentMember());
    }
}
