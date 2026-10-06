package com.lelcc.service;

import com.lelcc.dto.MemberInfoVO;
import com.lelcc.dto.MemberLoginDTO;
import com.lelcc.dto.MemberLoginVO;
import com.lelcc.dto.MemberProfileDTO;
import com.lelcc.dto.MemberRegisterDTO;

public interface AppAuthService {

    /** 玩家登录：校验用户名密码与封禁状态，签发 MEMBER 令牌。 */
    MemberLoginVO login(MemberLoginDTO dto);

    /** 玩家注册：用户名查重后落库并直接签发令牌（注册即登录）。 */
    MemberLoginVO register(MemberRegisterDTO dto);

    /** 当前登录玩家的信息，供用户端刷新页面后恢复登录态。 */
    MemberInfoVO currentMember();

    /** 修改当前登录玩家的昵称/简介/头像，返回更新后的信息。 */
    MemberInfoVO updateProfile(MemberProfileDTO dto);
}
