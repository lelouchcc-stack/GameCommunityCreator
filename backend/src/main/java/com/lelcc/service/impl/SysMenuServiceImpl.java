package com.lelcc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lelcc.common.BusinessException;
import com.lelcc.dto.MenuVO;
import com.lelcc.dto.SysMenuDTO;
import com.lelcc.entity.SysMenu;
import com.lelcc.mapper.SysMenuMapper;
import com.lelcc.mapper.SysRoleMenuMapper;
import com.lelcc.service.SysMenuService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class SysMenuServiceImpl implements SysMenuService {

    private static final long ROOT_PARENT_ID = 0L;
    private static final Set<String> MENU_TYPES = Set.of("M", "C", "F");
    private static final String TYPE_BUTTON = "F";
    private static final int VISIBLE_SHOW = 1;
    private static final int STATUS_ENABLED = 1;
    private static final int DEFAULT_SORT = 0;

    @Resource
    private SysMenuMapper sysMenuMapper;

    @Resource
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Override
    public List<MenuVO> listMenuTree(Long userId) {
        return buildTree(sysMenuMapper.selectVisibleMenusByUserId(userId), ROOT_PARENT_ID);
    }

    @Override
    public List<MenuVO> listAllMenuTree() {
        return buildTree(sysMenuMapper.selectAll(), ROOT_PARENT_ID);
    }

    @Override
    public void create(SysMenuDTO dto) {
        validateType(dto.menuType());
        Long parentId = normalizeParentId(dto.parentId());
        checkParentUsable(parentId);
        if (permsExists(dto.perms(), null)) {
            throw new BusinessException(400, "权限标识已存在");
        }

        SysMenu menu = new SysMenu();
        applyFields(menu, dto, parentId);
        sysMenuMapper.insert(menu);
    }

    @Override
    public void update(Long id, SysMenuDTO dto) {
        SysMenu exists = requireMenu(id);
        validateType(dto.menuType());
        Long parentId = normalizeParentId(dto.parentId());
        checkParentUsable(parentId);
        checkNoCycle(exists.getId(), parentId);
        if (permsExists(dto.perms(), id)) {
            throw new BusinessException(400, "权限标识已存在");
        }

        SysMenu menu = new SysMenu();
        menu.setId(exists.getId());
        applyFields(menu, dto, parentId);
        sysMenuMapper.updateById(menu);
    }

    @Override
    public void delete(Long id) {
        SysMenu menu = requireMenu(id);
        // 先拦住有子节点的，否则删完父节点子节点就成了挂在空中的孤儿
        if (sysMenuMapper.exists(Wrappers.<SysMenu>lambdaQuery().eq(SysMenu::getParentId, menu.getId()))) {
            throw new BusinessException(400, "该菜单下还有子节点，请先删除子节点");
        }
        long roleCount = sysRoleMenuMapper.countByMenuId(id);
        if (roleCount > 0) {
            throw new BusinessException(400, "该菜单已分配给 " + roleCount + " 个角色，请先解除关联");
        }
        sysMenuMapper.deleteById(id);
    }

    private void applyFields(SysMenu menu, SysMenuDTO dto, Long parentId) {
        menu.setParentId(parentId);
        menu.setMenuName(dto.menuName());
        menu.setMenuType(dto.menuType());
        menu.setPath(dto.path());
        menu.setComponent(dto.component());
        menu.setPerms(dto.perms());
        menu.setIcon(dto.icon());
        menu.setSort(dto.sort() == null ? DEFAULT_SORT : dto.sort());
        menu.setVisible(dto.visible() == null ? VISIBLE_SHOW : dto.visible());
        menu.setStatus(dto.status() == null ? STATUS_ENABLED : dto.status());
    }

    /** 一次查出扁平列表，在内存里按 parentId 递归成树，避免每层查一次库 */
    private List<MenuVO> buildTree(List<SysMenu> all, long parentId) {
        return all.stream()
                .filter(menu -> menu.getParentId() != null && menu.getParentId() == parentId)
                .sorted(Comparator.comparing(SysMenu::getSort, Comparator.nullsLast(Integer::compareTo)))
                .map(menu -> new MenuVO(
                        menu.getId(), menu.getParentId(), menu.getMenuName(), menu.getMenuType(),
                        menu.getPath(), menu.getComponent(), menu.getPerms(), menu.getIcon(), menu.getSort(),
                        menu.getVisible(), menu.getStatus(),
                        buildTree(all, menu.getId())))
                .toList();
    }

    private SysMenu requireMenu(Long id) {
        SysMenu menu = sysMenuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException(404, "菜单不存在");
        }
        return menu;
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null ? ROOT_PARENT_ID : parentId;
    }

    private void validateType(String menuType) {
        if (!MENU_TYPES.contains(menuType)) {
            throw new BusinessException(400, "菜单类型只能为 M（目录）、C（菜单）或 F（按钮）");
        }
    }

    /** 上级必须存在，且按钮下面不能再挂节点 */
    private void checkParentUsable(Long parentId) {
        if (parentId == ROOT_PARENT_ID) {
            return;
        }
        SysMenu parent = sysMenuMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException(400, "上级菜单不存在");
        }
        if (TYPE_BUTTON.equals(parent.getMenuType())) {
            throw new BusinessException(400, "按钮下不能再挂子节点");
        }
    }

    /** 不允许把自己挂到自己的子孙下，否则这棵子树会从根节点上脱落 */
    private void checkNoCycle(Long id, Long parentId) {
        Long current = parentId;
        while (current != null && current != ROOT_PARENT_ID) {
            if (current.equals(id)) {
                throw new BusinessException(400, "上级菜单不能是自己或自己的子节点");
            }
            SysMenu parent = sysMenuMapper.selectById(current);
            current = parent == null ? null : parent.getParentId();
        }
    }

    /** 权限标识唯一校验；为空表示不设权限标识，不参与校验 */
    private boolean permsExists(String perms, Long excludeId) {
        if (!StringUtils.hasText(perms)) {
            return false;
        }
        return sysMenuMapper.exists(Wrappers.<SysMenu>lambdaQuery()
                .eq(SysMenu::getPerms, perms)
                .ne(excludeId != null, SysMenu::getId, excludeId));
    }
}
