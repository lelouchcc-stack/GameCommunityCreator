package com.lelcc.service;

import com.lelcc.dto.TemplateApplyDTO;
import com.lelcc.dto.TemplateApplyVO;
import com.lelcc.dto.TemplateVO;

import java.util.List;

/**
 * 社区模板：把「一份模板数据」变成「一个可用的社区」。
 *
 * <p>模板数据放在 classpath:templates/&lt;code&gt;/ 下（manifest + 站点配置 + 组件库定义 +
 * 各页面节点树 + 初始内容），这里只负责读取、替换输入占位符、并经由既有的
 * UiPageService / SiteService 写库——不在模板里另起一套写库逻辑，页面节点的父子关系、
 * 属性白名单、值类型校验统统复用保存接口那一套。</p>
 */
public interface TemplateService {

    /** 可用模板清单（含各自的输入项声明），供管理端动态渲染「创建社区」表单 */
    List<TemplateVO> list();

    /**
     * 应用模板：写站点配置 → upsert 组件库 → 建/重建模板页面 → 按需写入初始内容。
     * 幂等：重复应用不会重复插数据，也不会把已有的真实内容改掉（除非显式选择覆盖页面）。
     */
    TemplateApplyVO apply(TemplateApplyDTO dto);
}
