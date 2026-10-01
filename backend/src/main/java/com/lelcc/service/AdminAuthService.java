package com.lelcc.service;

import com.lelcc.dto.AdminLoginDTO;
import com.lelcc.dto.AdminLoginVO;
import com.lelcc.dto.AdminUserInfoVO;

public interface AdminAuthService {

    /**
     * 后台账号登录：校验账号密码与启用状态，签发 ADMIN 令牌。
     */
    AdminLoginVO login(AdminLoginDTO dto);

    /** 当前登录账号的信息，供前端刷新页面后恢复账号信息 */
    AdminUserInfoVO currentUser();
}
