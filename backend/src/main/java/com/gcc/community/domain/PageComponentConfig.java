package com.gcc.community.domain;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 页面里一个组件的配置。
 */
public record PageComponentConfig(
        String id,
        String type,
        String title,
        boolean visible,
        String dataSource,      // 这类组件固定的数据源，有些类型是 null
        Map<String, Object> props,  // 只放简单值，不能嵌套对象或数组
        @JsonInclude(JsonInclude.Include.NON_NULL) PageAction action) {  // 只有 button 类型有

    /** 写代码时少传 action 用的构造方法。 */
    public PageComponentConfig(String id, String type, String title, boolean visible,
            String dataSource, Map<String, Object> props) {
        this(id, type, title, visible, dataSource, props, null);
    }
}
