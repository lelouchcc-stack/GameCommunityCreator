package com.lelcc.controller;

import com.lelcc.common.PageResult;
import com.lelcc.common.Result;
import com.lelcc.dto.AppPostItemVO;
import com.lelcc.dto.MemberInfoVO;
import com.lelcc.dto.MemberProfileDTO;
import com.lelcc.service.AppAuthService;
import com.lelcc.service.PostService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 玩家个人相关接口。/app/member/** 不在放行名单里，全部需要 MEMBER 令牌。
 * 只提供「当前登录人」自己的数据，不接受外部传入的 memberId，避免越权读他人内容。
 */
@RestController
@RequestMapping("/app/member")
public class AppMemberController {

    @Resource
    private AppAuthService appAuthService;

    @Resource
    private PostService postService;

    @GetMapping("/me")
    public Result<MemberInfoVO> me() {
        return Result.success(appAuthService.currentMember());
    }

    /** 修改昵称/简介/头像，返回更新后的信息供页面就地刷新 */
    @PutMapping("/profile")
    public Result<MemberInfoVO> updateProfile(@RequestBody MemberProfileDTO dto) {
        return Result.success(appAuthService.updateProfile(dto));
    }

    /** 我的帖子：含待审核与已下架的，作者自己要看得见 */
    @GetMapping("/my-posts")
    public Result<PageResult<AppPostItemVO>> myPosts(@RequestParam(defaultValue = "1") long pageNum,
                                                     @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(postService.pageMine(pageNum, pageSize));
    }
}
