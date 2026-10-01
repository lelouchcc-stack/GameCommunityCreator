package com.lelcc.service;

import com.lelcc.dto.AdminLoginDTO;
import com.lelcc.dto.AdminLoginVO;

public interface AdminAuthService {

    /**
     * 后台账号登录：校验账号密码与启用状态，签发 ADMIN 令牌。
     */
    AdminLoginVO login(AdminLoginDTO dto);
}
