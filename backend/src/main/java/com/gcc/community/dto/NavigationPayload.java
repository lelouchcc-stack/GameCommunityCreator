package com.gcc.community.dto;

import java.util.List;

/** 导航整体替换的请求参数。 */
public record NavigationPayload(List<NavigationItemInput> items) {
}
