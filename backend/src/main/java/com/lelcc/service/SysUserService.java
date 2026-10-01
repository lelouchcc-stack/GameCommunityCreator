package com.lelcc.service;

import com.lelcc.common.PageResult;
import com.lelcc.dto.SysUserDTO;
import com.lelcc.dto.SysUserVO;

import java.util.List;

public interface SysUserService {

    PageResult<SysUserVO> page(String username, String phone, Integer status, long pageNum, long pageSize);

    /** 该账号已分配的角色 ID，用于编辑表单回显 */
    List<Long> listRoleIds(Long id);

    void create(SysUserDTO dto);

    void update(Long id, SysUserDTO dto);

    /** 启用(1)或停用(0) */
    void changeStatus(Long id, Integer status);

    void resetPassword(Long id, String password);

    void delete(Long id);
}
