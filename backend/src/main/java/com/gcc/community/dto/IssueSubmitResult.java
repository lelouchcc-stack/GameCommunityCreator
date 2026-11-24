package com.gcc.community.dto;

import java.time.LocalDateTime;

/** 提交问题反馈后返回的结果。 */
public record IssueSubmitResult(long id, String status, LocalDateTime createdAt) {
}
