package com.gcc.community.dto;

import java.util.List;

/** 导航列表的返回结果。 */
public record NavigationEnvelope(List<NavigationItemView> items) {
}
