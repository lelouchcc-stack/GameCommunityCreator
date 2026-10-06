package com.lelcc.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告列表/详情项。
 *
 * <p>后台与用户端共用一份形状（与 TagVO 同例）：用户端只读已发布的公告，
 * 那几条 SQL 只选 id/title/content/type/create_time，因此 sort/status/updateTime
 * 在用户端返回里是 null；后台分页 SQL 会把它们一起带出来。</p>
 */
@Data
public class NoticeVO {

    private Long id;

    private String title;

    private String content;

    /** 1公告 2版本更新说明 */
    private Integer type;

    /** 展示顺序（越小越前），仅后台下发 */
    private Integer sort;

    /** 1发布 0草稿，仅后台下发 */
    private Integer status;

    private LocalDateTime createTime;

    /** 仅后台下发 */
    private LocalDateTime updateTime;
}
