package com.lelcc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标在 Controller 的增删改方法上，由 OperLogAspect 自动写进 sys_oper_log。
 * 只标关键写操作，查询接口不记录，否则日志表会被读请求淹没。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperLog {

    /** 业务模块，如"板块管理" */
    String module();

    /** 操作描述，如"新增板块" */
    String operation();
}
