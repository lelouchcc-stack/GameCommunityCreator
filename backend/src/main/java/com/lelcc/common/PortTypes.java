package com.lelcc.common;

import java.util.Set;

/**
 * 端口类型系统：连线按类型匹配，杜绝运行期才炸。
 * 名单对应设计文档 portTypes；其中 listBoardVersion / listProgressStage / listReport
 * 只出现在原子输出里（板块详情、我的举报），一并收进来保持校验不漏。
 *
 * 名单必须覆盖**原子库里真实用到的每个类型**，否则预设声明同类型入参时会被
 * checkSchema 拦下（act.updateProfile 的 avatar 就曾是这种情况）：
 * 原子库去重后的类型清单以本类的 ALL 为准，改原子 inputs/outputs 时记得同步这里。
 *
 * 刻意不收 nodeRef：它只出现在 on.click/on.submit 与 act.openModal 这类**页面级**原子的
 * 设置项上，而预设跨页面复用、不允许引用具体页面组件（UiPresetServiceImpl.checkNoNodeRef）。
 * 不注册它，预设若声明 nodeRef 入参就会被直接拒绝——这正是想要的结果。
 */
public final class PortTypes {

    public static final Set<String> ALL = Set.of(
            "signal", "text", "number", "boolean", "image",
            "member", "board", "boardVersion", "progressStage", "post", "comment", "notice",
            "listPost", "listBoard", "listComment", "listNotice",
            "listBoardVersion", "listProgressStage", "listReport",
            "pageInfo", "result", "any");

    private PortTypes() {
    }

    public static boolean isKnown(String type) {
        return type != null && ALL.contains(type);
    }
}
