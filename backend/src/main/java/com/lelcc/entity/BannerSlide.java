package com.lelcc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 首页轮播图，对应 banner_slide。 */
@Data
@TableName("banner_slide")
public class BannerSlide {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题，叠加显示在图上，可空 */
    private String title;

    /** 副标题 / 一句话说明，可空 */
    private String subtitle;

    /** 图片：对象存储 objectKey 或完整地址 */
    private String image;

    /** 点击跳转地址，空表示图上不可点 */
    private String linkUrl;

    private Integer sort;

    /** 1展示 0隐藏 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
