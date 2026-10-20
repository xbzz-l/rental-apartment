package com.rental.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 房源实体（对应 apartment 表）
 */
@Data
@TableName("`apartment`")
public class Apartment {

    /** 房源ID（主键） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题 */
    private String title;

    /** 房源图片（逗号分隔的URL列表） */
    private String images;

    /** 户型，如：一室一厅 */
    private String layout;

    /** 面积（㎡） */
    private BigDecimal area;

    /** 月租金（元） */
    private Integer rent;

    /** 状态：可租 / 已租 */
    private String status;

    /** 配套设施（逗号分隔） */
    private String facilities;

    /** 位置信息 */
    private String address;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
