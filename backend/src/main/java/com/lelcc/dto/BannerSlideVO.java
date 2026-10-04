package com.lelcc.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 轮播图列表项：后台表格与用户端轮播共用。 */
@Data
public class BannerSlideVO {

    private Long id;

    private String title;

    private String subtitle;

    /** 图片：objectKey 或完整地址，展示前由前端按存储基址现拼 */
    private String image;

    private String linkUrl;

    private Integer sort;

    private Integer status;

    private LocalDateTime createTime;
}
