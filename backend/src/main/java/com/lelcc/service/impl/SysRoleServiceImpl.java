package com.lelcc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lelcc.common.BusinessException;
import com.lelcc.common.PageResult;
import com.lelcc.dto.SysRoleDTO;
import com.lelcc.entity.SysRole;
import com.lelcc.mapper.SysRoleMapper;
import com.lelcc.mapper.SysRoleMenuMapper;
import com.lelcc.mapper.SysUserRoleMapper;
import com.lelcc.service.SysRoleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class SysRoleServiceImpl implements SysRoleService {

    /** 内置超管角色标识，不允许删除 */
    private static final String BUILT_IN_ROLE_KEY = "admin";
    private static final int STATUS_ENABLED = 1;
    private static final int DEFAULT_SORT = 0;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    @Override
    public PageResult<SysRole> page(String roleName, Integer status, long pageNum, long pageSize) {
        LambdaQueryWrapper<SysRole> wrapper = Wrappers.<SysRole>lambdaQuery()
                .like(StringUtils.hasText(roleName), SysRole::getRoleName, roleName)
                .eq(status != null, SysRole::getStatus, status)
                .orderByAsc(SysRole::getSort)
                .orderByAsc(SysRole::getId);
        return PageResult.of(sysRoleMapper.selectPage(new Page<>(pageNum, pageSize), wrapper));
    }

    @Override
    public List<SysRole> options() {
        return sysRoleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getStatus, STATUS_ENABLED)
                .orderByAsc(SysRole::getSort));
    }

    @Override
    public void create(SysRoleDTO dto) {
        if (roleKeyExists(dto.roleKey(), null)) {
            throw new BusinessException(400, "角色标识已存在");
        }
        SysRole role = new SysRole();
        role.setRoleName(dto.roleName());
        role.setRoleKey(dto.roleKey());
        role.setSort(dto.sort() == null ? DEFAULT_SORT : dto.sort());
        role.setStatus(dto.status() == null ? STATUS_ENABLED : dto.status());
        role.setRemark(dto.remark());
        sysRoleMapper.insert(role);
    }

    @Override
    public void update(Long id, SysRoleDTO dto) {
        SysRole exists = requireRole(id);
        if (roleKeyExists(dto.roleKey(), id)) {
            throw new BusinessException(400, "角色标识已存在");
        }
        SysRole role = new SysRole();
        role.setId(exists.getId());
        role.setRoleName(dto.roleName());
        role.setRoleKey(dto.roleKey());
        role.setSort(dto.sort() == null ? DEFAULT_SORT : dto.sort());
        role.setStatus(dto.status() == null ? STATUS_ENABLED : dto.status());
        role.setRemark(dto.remark());
        sysRoleMapper.updateById(role);
    }

    @Override
    public List<Long> listMenuIds(Long roleId) {
        requireRole(roleId);
        return sysRoleMenuMapper.selectMenuIdsByRoleId(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        requireRole(roleId);
        // 覆盖式：先清空再插入，两步必须在一个事务里
        sysRoleMenuMapper.deleteByRoleId(roleId);
        if (menuIds != null && !menuIds.isEmpty()) {
            sysRoleMenuMapper.insertBatch(roleId, menuIds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysRole role = requireRole(id);
        if (BUILT_IN_ROLE_KEY.equals(role.getRoleKey())) {
            throw new BusinessException(400, "系统管理员角色不可删除");
        }
        long userCount = sysUserRoleMapper.countByRoleId(id);
        if (userCount > 0) {
            throw new BusinessException(400, "该角色已分配给 " + userCount + " 个账号，请先解除绑定");
        }
        sysRoleMapper.deleteById(id);
        sysRoleMenuMapper.deleteByRoleId(id);
    }

    private SysRole requireRole(Long id) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(404, "角色不存在");
        }
        return role;
    }

    /** 角色标识唯一校验，excludeId 用于编辑时排除自己 */
    private boolean roleKeyExists(String roleKey, Long excludeId) {
        return sysRoleMapper.exists(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleKey, roleKey)
                .ne(excludeId != null, SysRole::getId, excludeId));
    }
}
