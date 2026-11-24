package com.gcc.community.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 按钮点击后要做的事。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PageAction(
        String type,          // navigate / externalLink / none
        String targetPageId,  // type=navigate 时用
        String url,           // type=externalLink 时用
        Boolean openInNewTab) {

    public static final String TYPE_NAVIGATE = "navigate";
    public static final String TYPE_EXTERNAL_LINK = "externalLink";
    public static final String TYPE_NONE = "none";
}
