package com.lelcc.service.impl;

import com.lelcc.dto.MenuVO;
import com.lelcc.entity.SysMenu;
import com.lelcc.mapper.SysMenuMapper;
import com.lelcc.service.SysMenuService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class SysMenuServiceImpl implements SysMenuService {

    private static final long ROOT_PARENT_ID = 0L;

    @Resource
    private SysMenuMapper sysMenuMapper;

    @Override
    public List<MenuVO> listMenuTree(Long userId) {
        return buildTree(sysMenuMapper.selectVisibleMenusByUserId(userId), ROOT_PARENT_ID);
    }

    /** 一次查出扁平列表，在内存里按 parentId 递归成树，避免每层查一次库 */
    private List<MenuVO> buildTree(List<SysMenu> all, long parentId) {
        return all.stream()
                .filter(menu -> menu.getParentId() != null && menu.getParentId() == parentId)
                .sorted(Comparator.comparing(SysMenu::getSort, Comparator.nullsLast(Integer::compareTo)))
                .map(menu -> new MenuVO(
                        menu.getId(), menu.getParentId(), menu.getMenuName(), menu.getMenuType(),
                        menu.getPath(), menu.getComponent(), menu.getPerms(), menu.getIcon(), menu.getSort(),
                        buildTree(all, menu.getId())))
                .toList();
    }
}
