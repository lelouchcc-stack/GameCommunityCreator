package com.lelcc.service;

import com.lelcc.dto.NodeDTO;
import com.lelcc.dto.NodeVO;
import com.lelcc.dto.PageDTO;
import com.lelcc.dto.PageTreeVO;
import com.lelcc.dto.PageVO;

import java.util.List;

/** 用户端页面与页面节点（组件树）管理。 */
public interface UiPageService {

    List<PageVO> list();

    void create(PageDTO dto);

    void update(Long id, PageDTO dto);

    void delete(Long id);

    /** 管理端：该页面的节点树（含未显示节点，值为原始覆盖值） */
    List<NodeVO> listNodes(Long pageId);

    /** 整棵树保存：新增(id 为负数临时 id)/更新/删除(缺失即删) */
    List<NodeVO> saveNodes(Long pageId, List<NodeDTO> nodes);

    /** 用户端：按 pageKey 取启用页面的可见节点树，样式与内容已与默认值合并 */
    PageTreeVO userTree(String pageKey);
}
