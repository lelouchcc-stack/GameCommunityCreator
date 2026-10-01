package com.lelcc.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lelcc.common.BusinessException;
import com.lelcc.common.PageResult;
import com.lelcc.dto.SysUserDTO;
import com.lelcc.dto.SysUserVO;
import com.lelcc.entity.SysUser;
import com.lelcc.mapper.SysUserMapper;
import com.lelcc.mapper.SysUserRoleMapper;
import com.lelcc.service.SysUserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class SysUserServiceImpl implements SysUserService {

    /** 内置超管账号：不可改用户名、不可停用、不可删除，避免把自己锁在系统外面 */
    private static final String BUILT_IN_USERNAME = "admin";
    private static final int STATUS_ENABLED = 1;
    private static final int STATUS_DISABLED = 0;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    @Override
    public PageResult<SysUserVO> page(String username, String phone, Integer status, long pageNum, long pageSize) {
        Page<SysUserVO> page = new Page<>(pageNum, pageSize);
        return PageResult.of(sysUserMapper.selectUserPage(page, username, phone, status));
    }

    @Override
    public List<Long> listRoleIds(Long id) {
        requireUser(id);
        return sysUserRoleMapper.selectRoleIdsByUserId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(SysUserDTO dto) {
        if (usernameExists(dto.username(), null)) {
            throw new BusinessException(400, "用户名已被占用");
        }
        if (!StringUtils.hasText(dto.password())) {
            throw new BusinessException(400, "请输入初始密码");
        }
        SysUser user = new SysUser();
        user.setUsername(dto.username());
        user.setPassword(BCrypt.hashpw(dto.password()));
        user.setNickname(dto.nickname());
        user.setPhone(dto.phone());
        user.setEmail(dto.email());
        user.setStatus(dto.status() == null ? STATUS_ENABLED : dto.status());
        user.setRemark(dto.remark());
        sysUserMapper.insert(user);
        assignRoles(user.getId(), dto.roleIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SysUserDTO dto) {
        SysUser exists = requireUser(id);
        if (usernameExists(dto.username(), id)) {
            throw new BusinessException(400, "用户名已被占用");
        }
        if (isBuiltIn(exists) && !exists.getUsername().equals(dto.username())) {
            throw new BusinessException(400, "系统管理员账号不可修改用户名");
        }

        SysUser user = new SysUser();
        user.setId(exists.getId());
        user.setUsername(dto.username());
        user.setNickname(dto.nickname());
        user.setPhone(dto.phone());
        user.setEmail(dto.email());
        user.setStatus(dto.status() == null ? STATUS_ENABLED : dto.status());
        user.setRemark(dto.remark());
        sysUserMapper.updateById(user);
        assignRoles(id, dto.roleIds());
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        if (status == null || (status != STATUS_ENABLED && status != STATUS_DISABLED)) {
            throw new BusinessException(400, "状态只能为 1（启用）或 0（停用）");
        }
        SysUser user = requireUser(id);
        if (isBuiltIn(user) && status == STATUS_DISABLED) {
            throw new BusinessException(400, "系统管理员账号不可停用");
        }
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setStatus(status);
        sysUserMapper.updateById(update);
    }

    @Override
    public void resetPassword(Long id, String password) {
        SysUser user = requireUser(id);
        SysUser update = new SysUser();
        update.setId(user.getId());
        // 重置后存的是新密码的 BCrypt 哈希，明文不落库
        update.setPassword(BCrypt.hashpw(password));
        sysUserMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysUser user = requireUser(id);
        if (isBuiltIn(user)) {
            throw new BusinessException(400, "系统管理员不可删除");
        }
        sysUserMapper.deleteById(id);
        sysUserRoleMapper.deleteByUserId(id);
    }

    /** 覆盖式重新分配角色；roleIds 为空表示不分配任何角色 */
    private void assignRoles(Long userId, List<Long> roleIds) {
        sysUserRoleMapper.deleteByUserId(userId);
        if (roleIds != null && !roleIds.isEmpty()) {
            sysUserRoleMapper.insertBatch(userId, roleIds);
        }
    }

    private SysUser requireUser(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "账号不存在");
        }
        return user;
    }

    private boolean isBuiltIn(SysUser user) {
        return BUILT_IN_USERNAME.equals(user.getUsername());
    }

    /** 用户名唯一校验，excludeId 用于编辑时排除自己 */
    private boolean usernameExists(String username, Long excludeId) {
        return sysUserMapper.exists(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username)
                .ne(excludeId != null, SysUser::getId, excludeId));
    }
}
