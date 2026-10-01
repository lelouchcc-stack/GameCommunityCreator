package com.lelcc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 站点配置（键值对），对应 site_config。 */
@Data
@TableName("site_config")
public class SiteConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 配置键，取值见 SiteConfigRegistry */
    private String configKey;

    private String configValue;

    private String remark;

    private LocalDateTime updateTime;
}
