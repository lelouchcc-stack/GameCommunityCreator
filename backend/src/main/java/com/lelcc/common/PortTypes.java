package com.lelcc.common;

import java.util.Set;

/**
 * 端口类型系统：连线按类型匹配，杜绝运行期才炸。
 * 名单对应设计文档 portTypes；其中 listBoardVersion / listProgressStage / listReport
 * 只出现在原子输出里（板块详情、我的举报），一并收进来保持校验不漏。
 */
public final class PortTypes {

    public static final Set<String> ALL = Set.of(
            "signal", "text", "number", "boolean",
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
