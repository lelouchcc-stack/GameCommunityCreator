package com.lelcc.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lelcc.common.BusinessException;
import com.lelcc.common.UserContext;
import com.lelcc.common.UserType;
import com.lelcc.dto.AdminLoginDTO;
import com.lelcc.dto.AdminLoginVO;
import com.lelcc.dto.AdminUserInfoVO;
import com.lelcc.entity.SysRole;
import com.lelcc.entity.SysUser;
import com.lelcc.mapper.SysRoleMapper;
import com.lelcc.mapper.SysUserMapper;
import com.lelcc.service.AdminAuthService;
import com.lelcc.utils.JwtUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminAuthServiceImpl implements AdminAuthService {

    private static final int STATUS_ENABLED = 1;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private JwtUtils jwtUtils;

    @Override
    public AdminLoginVO login(AdminLoginDTO dto) {
        SysUser user = sysUserMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, dto.username()));

        // 账号不存在与密码错误返回同一句提示，避免暴露账号是否存在
        if (user == null || !BCrypt.checkpw(dto.password(), user.getPassword())) {
            throw new BusinessException(401, "账号或密码错误");
        }
        if (!Integer.valueOf(STATUS_ENABLED).equals(user.getStatus())) {
            throw new BusinessException(403, "该账号已被停用，请联系管理员");
        }

        String token = jwtUtils.createToken(user.getId(), UserType.ADMIN);
        return new AdminLoginVO(token, toUserInfo(user));
    }

    @Override
    public AdminUserInfoVO currentUser() {
        SysUser user = sysUserMapper.selectById(UserContext.getId());
        if (user == null || !Integer.valueOf(STATUS_ENABLED).equals(user.getStatus())) {
            throw new BusinessException(401, "登录状态已失效，请重新登录");
        }
        return toUserInfo(user);
    }

    private AdminUserInfoVO toUserInfo(SysUser user) {
        List<String> roles = sysRoleMapper.selectEnabledByUserId(user.getId())
                .stream()
                .map(SysRole::getRoleKey)
                .toList();
        return new AdminUserInfoVO(
                user.getId(), user.getUsername(), user.getNickname(), user.getAvatar(), roles);
    }
}
